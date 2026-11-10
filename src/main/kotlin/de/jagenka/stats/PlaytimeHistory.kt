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
import java.time.LocalDate

@Suppress("UNCHECKED_CAST")
private val PLAY_TIME_STAT_TYPE = Stats.CUSTOM as StatType<Any>
private const val PLAY_TIME_STAT_ID = "play_time"

@Serializable
private class PlaytimeHistoryEntry(
    /**
     * date (ISO-8601, e.g. "2026-11-10") -> player name -> that player's *total* playtime (in ticks) as of the
     * last snapshot taken that day.
     *
     * Minecraft only ever stores a running total per player, never a history, so this is the only piece of state
     * this feature needs to add: a daily snapshot of that total, to later diff against. A day is only ever written
     * for players who were actually seen that day, so a gap between two of a player's snapshots always means
     * "nothing changed", never "unknown" - the diff between any two of their snapshots is always exact.
     */
    var snapshotsByDate: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()
)

/**
 * Tracks each player's playtime *history*, so that "how much has X played in the last N days" can be answered -
 * something vanilla Minecraft has no concept of, since [Stats.PLAY_TIME] (via [Stats.CUSTOM]) only ever stores a
 * single running total per player.
 *
 * Rather than re-implementing playtime tracking (e.g. by hooking the tick that increments the vanilla stat, or by
 * timing join/leave ourselves), this just takes a daily snapshot of the *existing* total and diffs against it later.
 * That keeps this feature a thin, low-maintenance layer on top of the same stat the rest of the mod already reads
 * (see [StatUtil.getStatDataList]), rather than a second, parallel playtime tracker that could drift from it.
 *
 * [snapshotNow] is cheap (one stat file read per known player, no different from what `/playtime` already does on
 * every invocation) and is meant to be called often - on join, on disconnect, periodically, and on server
 * start/stop - so that a day's final snapshot is never more than about an hour stale.
 */
object PlaytimeHistory
{
    /**
     * how long history is kept around for. only needs to comfortably exceed the largest `days` anyone will
     * reasonably query for; old entries beyond this are dropped on every [store] to keep the file small.
     */
    private const val RETENTION_DAYS = 90L

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
        val cutoff = LocalDate.now().minusDays(RETENTION_DAYS)
        entry.snapshotsByDate.keys.removeAll { LocalDate.parse(it) < cutoff }
    }

    /**
     * records every known player's *current* total playtime under today's date, overwriting today's previous
     * snapshot if there was one. safe and cheap to call as often as needed.
     */
    fun snapshotNow()
    {
        val currentTotals = currentPlayTimeByPlayerName() ?: return

        val todaysSnapshot = entry.snapshotsByDate.getOrPut(LocalDate.now().toString()) { mutableMapOf() }
        currentTotals.forEach { (playerName, ticks) -> todaysSnapshot[playerName] = ticks }

        store()
    }

    /**
     * @return ticks [playerName] has played in the last [days] days (today inclusive), or `null` if this player is
     * not known at all (never seen, so there is no current total to diff against)
     */
    fun recentPlaytimeTicks(playerName: String, days: Int): Int?
    {
        val currentTotal = currentPlayTimeByPlayerName()?.get(playerName) ?: return null

        // the point in time "days" ago that the window starts at; we need the total as it stood right before that,
        // i.e. the latest snapshot at or before that date
        val windowStart = LocalDate.now().minusDays(days.toLong())

        val knownSnapshots = entry.snapshotsByDate.entries
            .mapNotNull { (date, snapshot) -> snapshot[playerName]?.let { LocalDate.parse(date) to it } }

        // no history at all for this player yet - we can't prove any of their total falls within the window
        if (knownSnapshots.isEmpty()) return 0

        val baseline = knownSnapshots.filter { it.first <= windowStart }.maxByOrNull { it.first }?.second
            // history doesn't reach as far back as the window start (e.g. this feature just started tracking) -
            // fall back to the oldest snapshot we do have. this underestimates rather than overestimates: anything
            // before our very first snapshot is treated as outside the window, since we have no proof otherwise
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
