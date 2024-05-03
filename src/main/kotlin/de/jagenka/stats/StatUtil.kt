@file:Suppress("UNCHECKED_CAST")

package de.jagenka.stats

import de.jagenka.StatDataException
import de.jagenka.StatDataExceptionType.*
import de.jagenka.UserRegistry
import de.jagenka.Util.durationToPrettyString
import de.jagenka.Util.trimDecimals
import de.jagenka.stats.StatQueryType.*
import net.minecraft.stat.StatFormatter
import net.minecraft.stat.StatType
import net.minecraft.stat.Stats
import net.minecraft.stat.Stats.*
import net.minecraft.util.Identifier
import java.util.*
import kotlin.math.max
import kotlin.time.Duration.Companion.hours

object StatUtil
{
    @Throws(StatDataException::class)
    fun getStatDataList(statType: StatType<Any>, id: String): List<StatData>
    {
        try
        {
            val identifier = Identifier(id)
            val registry = statType.registry
            val key = registry.get(identifier) ?: throw StatDataException(INVALID_ID)
            if (registry.getId(key) != identifier) throw StatDataException(INVALID_ID)
            val stat = statType.getOrCreateStat(key)

            return UserRegistry.getMinecraftProfiles()
                .mapNotNull { StatData(identifier, stat, it.name, (PlayerStatManager.getStatHandlerForPlayer(it.name)?.getStat(stat) ?: return@mapNotNull null)) }
        } catch (_: Exception)
        {
            throw StatDataException(EMPTY)
        }
    }

    fun getRelStat(stat: Int, playtime: Int): Double
    {
        return (if (playtime == 0) 0.0 else (stat.toDouble() * 72_000.0)) / playtime.toDouble() // converts ticks to hours (20*60*60)
    }

    fun getInverseRelStat(stat: Int, playtime: Int): Double
    {
        return (playtime.toDouble() / 72_000.0) / max(1.0, stat.toDouble()) // converts ticks to hours (20*60*60)
    }

    /**
     * @param excludeFilter exclude all entries where this will return true
     * @param valuation the resulting entries are sorted descending by this value. this is also used for determining global rank
     */
    private fun getAllStatInfoSorted(
        statType: StatType<Any>,
        id: String,
        excludeFilter: (stat: StatData, playtime: StatData) -> Boolean = { _, _ -> false },
        valuation: (stat: StatData, playtime: StatData) -> Double,
        ascending: Boolean? = false,
    ): List<Triple<Int, StatData, StatData>>
    {
        val data = getStatDataList(statType, id)
        if (data.isEmpty()) throw StatDataException(EMPTY)

        val playtimeData = getStatDataList(Stats.CUSTOM as StatType<Any>, "play_time")
        if (playtimeData.isEmpty()) throw StatDataException(EMPTY)

        var currentRank = 1
        var currentValue: Double? = null

        return data
            .mapNotNull {
                it to (playtimeData.find { playtimeData ->
                    playtimeData.playerName.equals(it.playerName, ignoreCase = true)
                } ?: return@mapNotNull null)
            }
            .filterNot { excludeFilter(it.first, it.second) }
            .sortedByDescending { (if (ascending == true) -1 else 1) * valuation(it.first, it.second) }
            .mapIndexed { index, (statData, playtimeData) ->
                val value = valuation(statData, playtimeData)
                if (value != currentValue)
                {
                    currentRank = index + 1
                    currentValue = value
                }

                Triple(currentRank, statData, playtimeData)
            }
    }

    fun formatRelStat(someStatData: StatData, stat: Int, playtime: Int): String
    {
        val relStat = getRelStat(stat, playtime)
        val result = when (someStatData.stat.formatter)
        {
            StatFormatter.TIME ->
            {
                "${(relStat / 720.0).trimDecimals(2)}%" // converts tick stats to hours but then to percent ((value*100)/(20*60*60))
            }

            StatFormatter.DISTANCE ->
            {
                "${(relStat / 100_000.0).trimDecimals(3)} km/h" // converts cm stats to km (value/(100*1000))
            }

            else ->
            {
                "${relStat.trimDecimals(3)}/h"
            }
        }
        return result.trimEnd()
    }

    fun formatInverseRelStat(someStatData: StatData, stat: Int, playtime: Int): String
    {
        val inverseRelStat = getInverseRelStat(stat, playtime) // hours per stat
        val result = when (someStatData.stat.formatter)
        {
            StatFormatter.TIME ->
            {
                "${((playtime.toDouble() * 100) / max(1.0, stat.toDouble())).trimDecimals(2)}%"
            }

            StatFormatter.DISTANCE ->
            {
                "${durationToPrettyString((inverseRelStat * 100000).hours)} /km" // *100000 because conversion to km
            }

            else ->
            {
                durationToPrettyString(inverseRelStat.hours)
            }
        }
        return result.trimEnd()
    }

    fun StatType<Any>.displayName(): String
    {
        return when (this)
        {
            MINED -> "mined"
            CRAFTED -> "crafted"
            USED -> "used"
            BROKEN -> "broken"
            PICKED_UP -> "picked up"
            DROPPED -> "dropped"
            KILLED -> "killed"
            KILLED_BY -> "killed by"
            CUSTOM -> ""
            else -> this.name.string
        }
    }
}