@file:Suppress("UNCHECKED_CAST")

package de.jagenka.stats

import de.jagenka.StatDataException
import de.jagenka.StatDataExceptionType.*
import de.jagenka.UserRegistry
import de.jagenka.Util.durationToPrettyString
import de.jagenka.Util.subListUntilOrEnd
import de.jagenka.Util.ticks
import de.jagenka.Util.trimDecimals
import de.jagenka.stats.StatUtil.StatQueryType.*
import net.minecraft.stat.StatFormatter
import net.minecraft.stat.StatType
import net.minecraft.stat.Stats
import net.minecraft.util.Identifier
import kotlin.math.max
import kotlin.time.Duration.Companion.hours

object StatUtil
{
    @Throws(StatDataException::class)
    private fun getStatDataList(statType: StatType<Any>, id: String): List<StatData>
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

    private fun getRelStat(stat: Int, playtime: Int): Double
    {
        return (if (playtime == 0) 0.0 else (stat.toDouble() * 72_000.0)) / playtime.toDouble() // converts ticks to hours (20*60*60)
    }

    private fun getInverseRelStat(stat: Int, playtime: Int): Double
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
        valuation: (stat: StatData, playtime: StatData) -> Double
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
            .sortedByDescending { valuation(it.first, it.second) }
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

    /**
     * @param nameFilter only display for those player names. don't filter if collection is empty
     */
    fun getStatReply(statType: StatType<Any>, id: String, queryType: StatQueryType, nameFilter: Collection<String> = emptyList(), limit: Int = 10): String
    {
        try
        {
            val untrimmedList = getAllStatInfoSorted(
                statType = statType,
                id = id,
                excludeFilter = { stat, _ ->
                    when (queryType)
                    {
                        TIME_PER_STAT -> stat.stat.formatter == StatFormatter.DISTANCE && stat.value == 0
                        else -> false
                    }
                },
                valuation = { stat, playtime ->
                    when (queryType)
                    {
                        DEFAULT, COMPARE -> stat.value
                        STAT_PER_TIME -> getRelStat(stat.value, playtime.value)
                        TIME_PER_STAT -> getInverseRelStat(stat.value, playtime.value)
                    }.toDouble()
                })
                .filter {
                    nameFilter.isEmpty() || nameFilter.map { it.lowercase() }.contains(it.second.playerName.lowercase())
                }
            val resultList = untrimmedList.subListUntilOrEnd(limit)

            var replyString = resultList.joinToString(
                separator = System.lineSeparator()
            ) { (rank, stat, playtime) ->
                when (queryType)
                {
                    // max length of player name is 16 character
                    DEFAULT, COMPARE -> "${rank.toString().padStart(2, ' ')}. ${stat.playerName.padEnd(17, ' ')} ${stat.stat.format(stat.value)}"
                        .padEnd(40) + if (!(id == "play_time" || statType == Stats.CUSTOM)) " (in ${durationToPrettyString(playtime.value.ticks)})" else ""

                    STAT_PER_TIME ->
                    {
                        val relStat = getRelStat(stat.value, playtime.value)
                        var result = "${rank.toString().padStart(2, ' ')}. ${stat.playerName.padEnd(17, ' ')} "
                        result +=
                            when (stat.stat.formatter)
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
                        result = result.padEnd(32, ' ') // 17 from above plus 15 (should be enough spacing)
                        result += " (${stat.stat.format(stat.value)} in ${durationToPrettyString(playtime.value.ticks)})"
                        result
                    }

                    TIME_PER_STAT ->
                    {
                        val inverseRelStat = getInverseRelStat(stat.value, playtime.value) // hours per stat
                        var result = "${rank.toString().padStart(2, ' ')}. ${stat.playerName.padEnd(17, ' ')} "
                        result +=
                            when (stat.stat.formatter)
                            {
                                StatFormatter.TIME ->
                                {
                                    "${((playtime.value.toDouble() * 100) / max(1.0, stat.value.toDouble())).trimDecimals(2)}%"
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
                        result = result.padEnd(44, ' ')
                        result += " (${durationToPrettyString(playtime.value.ticks)} for ${stat.stat.format(stat.value)})"
                        result
                    }
                }
            }

            if (untrimmedList.size > resultList.size)
            {
                replyString += "\n  and ${untrimmedList.size - resultList.size} more."
            }

            return replyString
        } catch (e: StatDataException)
        {
            return e.type.response
        }
    }

    enum class StatQueryType
    {
        DEFAULT, STAT_PER_TIME, TIME_PER_STAT, COMPARE
    }
}