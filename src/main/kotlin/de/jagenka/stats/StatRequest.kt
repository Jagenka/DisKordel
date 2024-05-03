package de.jagenka.stats

import de.jagenka.StatDataException
import de.jagenka.StatDataExceptionType.EMPTY
import de.jagenka.Util.durationToPrettyString
import de.jagenka.Util.percent
import de.jagenka.Util.ticks
import de.jagenka.Util.trimDecimals
import de.jagenka.stats.StatQueryType.*
import de.jagenka.stats.StatUtil.getInverseRelStat
import de.jagenka.stats.StatUtil.getRelStat
import de.jagenka.stats.StatUtil.getStatDataList
import net.minecraft.stat.StatFormatter
import net.minecraft.stat.StatType
import net.minecraft.stat.Stats
import kotlin.math.max
import kotlin.time.Duration.Companion.hours

class StatRequest(
    val statType: StatType<Any>,
    val id: String,
    val queryType: StatQueryType,
    ascending: Boolean = false,
    partOfName: String? = null,
    topN: Int? = null,
)
{
    private val valuation: (stat: StatData, playtime: StatData) -> Double = { stat, playtime ->
        (if (ascending) -1 else 1) *
                when (queryType)
                {
                    DEFAULT, COMPARE -> stat.value
                    STAT_PER_TIME -> getRelStat(stat.value, playtime.value)
                    TIME_PER_STAT -> getInverseRelStat(stat.value, playtime.value)
                }.toDouble()
    }

    val statEntries: List<StatData> = getStatDataList(statType, id)
    val playtimeEntries: List<StatData> = getStatDataList(Stats.CUSTOM as StatType<Any>, "play_time")

    val serverTotalStat = statEntries.sumOf { it.value }
    val serverTotalPlaytime = playtimeEntries.sumOf { it.value }

    init
    {
        if (statEntries.isEmpty()) throw StatDataException(EMPTY)
        if (playtimeEntries.isEmpty()) throw StatDataException(EMPTY)

        // group
        var requestResult = mutableListOf<StatRequestResultRow>()
        statEntries.forEach { statData ->
            val playerName = statData.playerName
            val playtimeData = playtimeEntries.find { it.playerName == playerName }
            if (playtimeData != null)
            {
                requestResult.add(StatRequestResultRow(null, playerName, statData, playtimeData))
            }
        }
        // sort
        requestResult = requestResult.sortedByDescending { valuation(it.stat, it.playtime) }.toMutableList()

        // filter
        // enumerate
    }
}

data class StatRequestResultRow(val rank: Int?, val playerName: String, val stat: StatData, val playtime: StatData)
{
    fun toString(queryType: StatQueryType, totalStat: Int): String
    {
        return when (queryType)
        {
            // max length of player name is 16 character
            DEFAULT, COMPARE -> "${rank.toString().padStart(2, ' ')}. ${stat.playerName.padEnd(17, ' ')} ${stat.stat.format(stat.value)}"
                .padEnd(38) + " (${(stat.value.toDouble() / totalStat).percent(3)})"

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
                result = result.padEnd(38, ' ')
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
                result = result.padEnd(38, ' ')
                result += " (${durationToPrettyString(playtime.value.ticks)} for ${stat.stat.format(stat.value)})"
                result
            }
        }.trimEnd()
    }
}