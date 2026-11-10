package de.jagenka.stats

import de.jagenka.MinecraftHandler
import de.jagenka.StatDataException
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.stats.StatType
import net.minecraft.stats.Stats
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

@Suppress("UNCHECKED_CAST")
private val PLAY_TIME_STAT_TYPE = Stats.CUSTOM as StatType<Any>
private const val PLAY_TIME_STAT_ID = "play_time"

@Serializable
private class PlaytimeHistoryEntry(
    /**
     * snapshot instant (ISO-8601, e.g. "2026-11-10T15:30:00.123Z") -> player name -> that player's *total*
     * playtime (in ticks) at that instant.
     *
     * Minecraft only ever stores a running total per player, never a history, so this is the only piece of state
     * this feature needs to add: enough timestamped snapshots of that total to diff against later. Every call to
     * [PlaytimeHistory.snapshotNow] just adds a new entry here - there's no day-keyed "first one wins" bookkeeping;
     * at query time, whichever existing entry is the best fit for the requested window gets picked (see
     * [PlaytimeHistory.recentPlaytimeTicks]).
     */
    var snapshots: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()
)

/**
 * Tracks each player's playtime *history*, so that "how much has X played in the last N days" can be answered -
 * something vanilla Minecraft has no concept of, since [Stats.PLAY_TIME] (via [Stats.CUSTOM]) only ever stores a
 * single running total per player.
 *
 * Rather than re-implementing playtime tracking (e.g. by hooking the tick that increments the vanilla stat, or by
 * timing join/leave ourselves), this just takes timestamped snapshots of the *existing* total and diffs against
 * them later. That keeps this feature a thin, low-maintenance layer on top of the same stat the rest of the mod
 * already reads (see [StatUtil.getStatDataList]), rather than a second, parallel playtime tracker that could drift
 * from it.
 *
 * [snapshotNow] is called on every join, disconnect, and server start/stop, plus on a timer (see [POLL_INTERVAL])
 * while anyone is online. None of this is about calendar days specifically - it's just about keeping a reasonably
 * dense trail of snapshots, so that whatever `days` someone queries for later, there's a snapshot close to that
 * window's boundary to diff against. A server that restarts multiple times a day, or one that's only up for a few
 * hours, works the same way: every snapshot is its own independent data point.
 *
 * [snapshotNow] itself is not free - it reads every known player's stat file (same work `/playtime` already does
 * on every invocation, just now also happening on its own off the game loop) - so [MIN_INTERVAL_BETWEEN_SNAPSHOTS]
 * caps how often it actually does anything, regardless of how often it's called. That matters because join and
 * disconnect fire on the server thread: calling this *directly* from those callbacks would block the game loop on
 * disk reads, and on a server with many players or a bursty reconnect pattern, that's a real, repeated lag source,
 * not just a one-off cost. Callers must dispatch it onto a coroutine (see `MinecraftHandler.registerMixins`)
 * instead of calling it inline.
 *
 * That also means join, disconnect, the hourly timer, and a Discord command reading [recentPlaytimeTicks] can all
 * run on different threads at once (`Main.scope` isn't confined to one), so every method here is synchronized -
 * the backing map is a plain, non-thread-safe `MutableMap`, and without that, concurrent access to it could throw
 * or silently corrupt state, not just race on which snapshot wins.
 */
object PlaytimeHistory
{
    /**
     * how long history is kept around for. only needs to comfortably exceed the largest `days` anyone will
     * reasonably query for; old entries beyond this are dropped once a day (see [pruneOldEntriesIfDueLocked]) to
     * keep the file small.
     */
    private val RETENTION: Duration = Duration.ofDays(90)

    /**
     * how often [snapshotNow] is called on a timer while the server is running (see [MinecraftHandler]). this
     * bounds how far a window boundary can fall from the nearest snapshot taken while someone was actually online
     * to cause a change - see [recentPlaytimeTicks].
     */
    val POLL_INTERVAL: Duration = Duration.ofHours(1)

    /**
     * the minimum time between two snapshots actually being taken, regardless of how often [snapshotNow] itself
     * is called. [POLL_INTERVAL] already guarantees a snapshot at least once an hour, so anything faster than that
     * is purely extra freshness, not something [recentPlaytimeTicks]'s correctness depends on - it's not worth
     * repeating the per-player stat file reads for every single join/disconnect on a busy server, especially
     * several of which can fire within the same second.
     */
    private val MIN_INTERVAL_BETWEEN_SNAPSHOTS: Duration = Duration.ofMinutes(5)

    /**
     * guards every access to [entry] and the throttling timestamps below, since join, disconnect, the hourly
     * timer, and Discord commands reading this can all call into this object from different threads at once.
     * everything guarded by it is plain in-memory map work plus occasional small file I/O, so contention is a
     * non-issue - this is about correctness, not performance.
     */
    private val lock = Any()

    private val serializer = Json { prettyPrint = true }
    private lateinit var pathToFile: Path
    private var entry = PlaytimeHistoryEntry()
    private var lastSnapshotAt: Instant? = null
    private var lastPrunedAt: Instant? = null

    fun load(): Unit = synchronized(lock) {
        pathToFile = FabricLoader.getInstance().configDir.resolve("diskordel_playtime_history.json")

        if (!Files.exists(pathToFile))
        {
            storeLocked()
            return
        }

        entry = try
        {
            serializer.decodeFromString(Files.readString(pathToFile))
        } catch (e: Exception)
        {
            MinecraftHandler.logger.error("Could not load playtime history, starting a new one. (${e.message})")
            PlaytimeHistoryEntry()
        }
    }

    private fun store() = synchronized(lock) { storeLocked() }

    // callers must already hold [lock] - exists only so snapshotNow() and load() don't have to re-acquire it
    // (a JVM monitor is re-entrant for the same thread, but going through synchronized() again is needless noise)
    private fun storeLocked()
    {
        pruneOldEntriesIfDueLocked()
        Files.writeString(pathToFile, serializer.encodeToString(entry))
    }

    /**
     * pruning itself means scanning and re-parsing every stored instant, so - like [snapshotNow] - it's throttled
     * to not redo that on every single write once the history grows large. Once a day is as often as it needs to
     * run, since [RETENTION] is measured in whole days to begin with.
     */
    private fun pruneOldEntriesIfDueLocked()
    {
        val now = Instant.now()
        lastPrunedAt?.let { if (Duration.between(it, now) < Duration.ofDays(1)) return }
        lastPrunedAt = now

        val cutoff = now.minus(RETENTION)
        entry.snapshots.keys.removeAll { Instant.parse(it) < cutoff }
    }

    /**
     * records every known player's *current* total playtime as a new snapshot, unless one was already taken more
     * recently than [MIN_INTERVAL_BETWEEN_SNAPSHOTS] - in which case this does (almost) nothing, cheaply. Must be
     * called off the server thread (e.g. `Main.scope.launch { ... }`), since the stat file reads behind it are not
     * free - see the class-level docs.
     */
    fun snapshotNow(): Unit = synchronized(lock) {
        val now = Instant.now()
        lastSnapshotAt?.let { if (Duration.between(it, now) < MIN_INTERVAL_BETWEEN_SNAPSHOTS) return }
        lastSnapshotAt = now

        val currentTotals = currentPlayTimeByPlayerName() ?: return

        entry.snapshots[now.toString()] = currentTotals.toMutableMap()

        storeLocked()
    }

    /**
     * @return ticks [playerName] has played in the last [days] days - i.e. since `now - days*24h`, not since the
     * start of some calendar day - or `null` if this player isn't known at all (never seen, so there is no
     * current total to diff against)
     */
    fun recentPlaytimeTicks(playerName: String, days: Int): Int? = synchronized(lock) {
        val currentTotal = currentPlayTimeByPlayerName()?.get(playerName) ?: return null

        val windowStart = Instant.now().minus(Duration.ofDays(days.toLong()))

        val knownSnapshots = entry.snapshots.entries
            .mapNotNull { (instant, snapshot) -> snapshot[playerName]?.let { Instant.parse(instant) to it } }

        // no history at all for this player yet - we can't prove any of their total falls within the window
        if (knownSnapshots.isEmpty()) return 0

        // the closest snapshot at or before the window's start is the correct baseline to diff against. if our
        // history doesn't reach back that far yet (e.g. this feature only just started tracking), fall back to
        // the oldest snapshot we do have instead - this underestimates rather than overestimates, since nothing
        // before our very first snapshot can be proven to fall inside the window
        val baseline = knownSnapshots.filter { it.first <= windowStart }.maxByOrNull { it.first }?.second
            ?: knownSnapshots.minByOrNull { it.first }!!.second

        (currentTotal - baseline).coerceAtLeast(0)
    }

    private fun currentPlayTimeByPlayerName(): Map<String, Int>? = try
    {
        StatUtil.getStatDataList(PLAY_TIME_STAT_TYPE, PLAY_TIME_STAT_ID).associate { it.playerName to it.value }
    } catch (_: StatDataException)
    {
        null
    }
}
