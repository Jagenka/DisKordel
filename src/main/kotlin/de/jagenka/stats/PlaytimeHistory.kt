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
 * [snapshotNow] is cheap (one stat file read per known player, no different from what `/playtime` already does on
 * every invocation), and is called on every join, disconnect, and server start/stop, plus on a timer (see
 * [POLL_INTERVAL]) while anyone is online. None of this is about calendar days specifically - it's just about
 * keeping a reasonably dense trail of snapshots, so that whatever `days` someone queries for later, there's a
 * snapshot close to that window's boundary to diff against. A server that restarts multiple times a day, or one
 * that's only up for a few hours, works the same way: every snapshot is its own independent data point.
 */
object PlaytimeHistory
{
    /**
     * how long history is kept around for. only needs to comfortably exceed the largest `days` anyone will
     * reasonably query for; old entries beyond this are dropped on every [store] to keep the file small.
     */
    private val RETENTION: Duration = Duration.ofDays(90)

    /**
     * how often [snapshotNow] is called on a timer while the server is running (see [MinecraftHandler]). this
     * bounds how far a window boundary can fall from the nearest snapshot taken while someone was actually online
     * to cause a change - see [recentPlaytimeTicks].
     */
    val POLL_INTERVAL: Duration = Duration.ofHours(1)

    private val serializer = Json { prettyPrint = true }
    private lateinit var pathToFile: Path
    private var entry = PlaytimeHistoryEntry()

    fun load()
    {
        pathToFile = FabricLoader.getInstance().configDir.resolve("diskordel_playtime_history.json")

        if (!Files.exists(pathToFile))
        {
            store()
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

    private fun store()
    {
        pruneOldEntries()
        Files.writeString(pathToFile, serializer.encodeToString(entry))
    }

    private fun pruneOldEntries()
    {
        val cutoff = Instant.now().minus(RETENTION)
        entry.snapshots.keys.removeAll { Instant.parse(it) < cutoff }
    }

    /**
     * records every known player's *current* total playtime under this exact instant, as a new entry - safe and
     * cheap to call as often as needed. Callers on a timer (as opposed to a join/disconnect/server start/stop
     * event, each meaningful on its own) should skip calling this while no-one is online, since nothing could have
     * changed since the last snapshot anyway.
     */
    fun snapshotNow()
    {
        val currentTotals = currentPlayTimeByPlayerName() ?: return

        entry.snapshots[Instant.now().toString()] = currentTotals.toMutableMap()

        store()
    }

    /**
     * @return ticks [playerName] has played in the last [days] days - i.e. since `now - days*24h`, not since the
     * start of some calendar day - or `null` if this player isn't known at all (never seen, so there is no
     * current total to diff against)
     */
    fun recentPlaytimeTicks(playerName: String, days: Int): Int?
    {
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

        return (currentTotal - baseline).coerceAtLeast(0)
    }

    private fun currentPlayTimeByPlayerName(): Map<String, Int>? = try
    {
        StatUtil.getStatDataList(PLAY_TIME_STAT_TYPE, PLAY_TIME_STAT_ID).associate { it.playerName to it.value }
    } catch (_: StatDataException)
    {
        null
    }
}
