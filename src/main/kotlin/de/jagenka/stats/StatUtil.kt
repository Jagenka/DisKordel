@file:Suppress("UNCHECKED_CAST")

package de.jagenka.stats

import de.jagenka.StatDataException
import de.jagenka.StatDataExceptionType.EMPTY
import de.jagenka.StatDataExceptionType.INVALID_ID
import de.jagenka.UserRegistry
import de.jagenka.Util.durationToPrettyString
import de.jagenka.Util.ticksToPrettyString
import de.jagenka.Util.trimDecimals
import net.minecraft.stat.StatType
import net.minecraft.stat.Stats
import net.minecraft.stat.Stats.*
import net.minecraft.util.Identifier
import kotlin.math.max
import kotlin.time.Duration.Companion.hours

object StatUtil
{
    val distanceStatIds = listOf(
        "climb_one_cm",
        "crouch_one_cm",
        "fall_one_cm",
        "fly_one_cm",
        "sprint_one_cm",
        "swim_one_cm",
        "walk_one_cm",
        "walk_on_water_one_cm",
        "walk_under_water_one_cm",
        "boat_one_cm",
        "aviate_one_cm",
        "horse_one_cm",
        "minecart_one_cm",
        "pig_one_cm",
        "strider_one_cm"
    )

    @Throws(StatDataException::class)
    fun getStatDataList(statType: StatType<Any>, id: String): List<StatData>
    {
        if (id == "speed")
        {
            return distanceStatIds.map { statId ->
                try
                {
                    val identifier = Identifier.of(statId)
                    val registry = statType.registry
                    val key = registry.get(identifier) ?: throw StatDataException(INVALID_ID)
                    if (registry.getId(key) != identifier) throw StatDataException(INVALID_ID)
                    val stat = statType.getOrCreateStat(key)

                    return@map UserRegistry
                        .getMinecraftProfiles()
                        .mapNotNull {
                            StatData(
                                statId,
                                it.name,
                                (PlayerStatManager.getStatHandlerForPlayer(it.name)?.getStat(stat) ?: return@mapNotNull null),
                                StatDataType.fromFormatter(stat.formatter)
                            )
                        }
                } catch (_: Exception)
                {
                    throw StatDataException(EMPTY)
                }
            }
                .flatten()
                .groupBy { it.playerName }
                .map { (playerName, statDataList) ->
                    val newValue = statDataList.sumOf { it.value }
                    return@map StatData("speed", playerName, newValue, StatDataType.DISTANCE)
                }
        } else
        {
            try
            {
                val identifier = Identifier.of(id)
                val registry = statType.registry
                val key = registry.get(identifier) ?: throw StatDataException(INVALID_ID)
                if (registry.getId(key) != identifier) throw StatDataException(INVALID_ID)
                val stat = statType.getOrCreateStat(key)

                return UserRegistry.getMinecraftProfiles()
                    .mapNotNull {
                        StatData(
                            id,
                            it.name,
                            (PlayerStatManager.getStatHandlerForPlayer(it.name)?.getStat(stat) ?: return@mapNotNull null),
                            StatDataType.fromFormatter(stat.formatter),
                            formatter = stat.formatter
                        )
                    }
            } catch (_: Exception)
            {
                throw StatDataException(EMPTY)
            }
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

    fun formatStat(statData: StatData, value: Int = statData.value): String
    {
        return when (statData.type)
        {
            StatDataType.TIME ->
            {
                ticksToPrettyString(value)
            }

            StatDataType.DISTANCE ->
            {
                "${(value.toDouble() / 100_000.0).trimDecimals(3)} km" // converts cm stats to km (value/(100*1000))
            }

            else ->
            {
                if (statData.formatter != null)
                {
                    statData.formatter.format(value)
                } else
                {
                    value.toString()
                }
            }
        }
    }

    fun formatRelStat(someStatData: StatData, stat: Int, playtime: Int): String
    {
        val relStat = getRelStat(stat, playtime)
        val result = when (someStatData.type) // TODO move to StatData?
        {
            StatDataType.TIME ->
            {
                "${(relStat / 720.0).trimDecimals(2)}%" // converts tick stats to hours but then to percent ((value*100)/(20*60*60))
            }

            StatDataType.DISTANCE ->
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
        val result = when (someStatData.type)
        {
            StatDataType.TIME ->
            {
                "${((playtime.toDouble() * 100) / max(1.0, stat.toDouble())).trimDecimals(2)}%"
            }

            StatDataType.DISTANCE ->
            {
                "${durationToPrettyString((inverseRelStat * 100000).hours)} /km" // *100,000 because conversion to km
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