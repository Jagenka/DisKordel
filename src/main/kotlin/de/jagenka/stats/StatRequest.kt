package de.jagenka.stats

import com.mojang.authlib.GameProfile
import de.jagenka.DiscordHandler
import de.jagenka.DiscordHandler.asCodeBlock
import de.jagenka.StatDataException
import de.jagenka.StatDataExceptionType.EMPTY
import de.jagenka.Util.code
import de.jagenka.Util.durationToPrettyString
import de.jagenka.Util.percent
import de.jagenka.Util.subListUntilOrEnd
import de.jagenka.Util.ticks
import de.jagenka.Util.trimDecimals
import de.jagenka.stats.StatQueryType.*
import de.jagenka.stats.StatUtil.displayName
import de.jagenka.stats.StatUtil.formatInverseRelStat
import de.jagenka.stats.StatUtil.formatRelStat
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
    ascending: Boolean,
    profileFilter: Collection<GameProfile>,
    val topN: Int?,
    val invoker: GameProfile?
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

    private val nameFilter = profileFilter

    val statEntries: List<StatData> = getStatDataList(statType, id)
    val playtimeEntries: List<StatData> = getStatDataList(Stats.CUSTOM as StatType<Any>, "play_time")

    val serverTotalStat = statEntries.sumOf { it.value }
    val serverTotalPlaytime = playtimeEntries.sumOf { it.value }

    val requestResult: List<StatRequestResultRow>

    init
    {
        if (statEntries.isEmpty()) throw StatDataException(EMPTY)
        if (playtimeEntries.isEmpty()) throw StatDataException(EMPTY)

        val requestResultUnfinished = statEntries
            // group
            .mapNotNull { statData ->
                val playerName = statData.playerName
                val playtimeData = playtimeEntries.find { it.playerName.equals(playerName, ignoreCase = true) }

                if (playtimeData == null) return@mapNotNull null
                return@mapNotNull StatRequestResultRow(playerName, statData, playtimeData)
            }
            // sort
            .sortedByDescending { valuation(it.stat, it.playtime) }
            // filter to avoid dividing by zero
            .filterNot {
                queryType == TIME_PER_STAT && it.stat.stat.formatter == StatFormatter.DISTANCE && it.stat.value == 0
            }


        // enumerate
        var currentRank = 1
        var currentValue: Double? = null
        requestResultUnfinished.forEachIndexed { index, row ->
            val value = valuation(row.stat, row.playtime)
            if (value != currentValue)
            {
                currentRank = index + 1
                currentValue = value
            }
            row.setRank(currentRank)
        }

        // filter according to partOfName given
        requestResult = requestResultUnfinished.filter {
            nameFilter.isEmpty() || nameFilter.map { it.name.lowercase() }.contains(it.playerName.lowercase())
        }.toList()
    }

    fun getTrimmedStringsList(): List<String>
    {
        val indexOfInvoker = requestResult.indexOfFirst { it.playerName == invoker?.name }.takeIf { it != -1 }

        val allStrings = requestResult.map { it.toString(queryType, serverTotalStat) }

        return if (topN == null)
        {
            return if (indexOfInvoker != null && allStrings.sumOf { it.length + 1 } > DiscordHandler.MESSAGE_LENGTH_LIMIT)
            {
                val trimmedLines = mutableListOf<String>()

                val topTrim = 3
                val middleTopTrim = indexOfInvoker - 1
                val middleBotTrim = indexOfInvoker + 2
                val botTrim = allStrings.size - 3


                if (topTrim + 2 < middleTopTrim)
                {
                    trimmedLines.addAll(allStrings.subList(0, topTrim))
                    trimmedLines.addAll(listOf("  .", "  ."))
                    trimmedLines.addAll(allStrings.subList(middleTopTrim, indexOfInvoker + 1))
                } else
                {
                    trimmedLines.addAll(allStrings.subList(0, indexOfInvoker + 1))
                }

                if (middleBotTrim + 2 < botTrim)
                {
                    trimmedLines.addAll(allStrings.subList(indexOfInvoker + 1, middleBotTrim))
                    trimmedLines.addAll(listOf(".", "."))
                    trimmedLines.addAll(allStrings.subList(botTrim, allStrings.size))
                } else
                {
                    trimmedLines.addAll(allStrings.subList(indexOfInvoker + 1, allStrings.size))
                }

                trimmedLines
            } else allStrings
        } else
        {
            allStrings.subListUntilOrEnd(max(topN, 1))
        }
    }

    fun getTotalString(): String?
    {
        val someStatData = requestResult.firstOrNull()?.stat

        return if (someStatData == null) null
        else
        {
            when (queryType)
            {
                DEFAULT, COMPARE ->
                {
                    val formattedValue = someStatData.stat.format(serverTotalStat)
                    if (formattedValue != null) "server total: ${formattedValue.code()}" else null
                }

                STAT_PER_TIME ->
                {
                    "server average: " + formatRelStat(someStatData, serverTotalStat, serverTotalPlaytime).code()
                }

                TIME_PER_STAT ->
                {
                    "server average: " + formatInverseRelStat(someStatData, serverTotalStat, serverTotalPlaytime).code()
                }
            }
        }
    }

    fun getReplyString(): String
    {
        val statTypeDisplayName = statType.displayName()
        val trimmedStringsList = getTrimmedStringsList()
        val statsString = trimmedStringsList.joinToString(separator = System.lineSeparator())
        val totalString = getTotalString()

        var replyString = "# " + (statTypeDisplayName + (if (statTypeDisplayName.isNotEmpty()) ": " else "") +
                id.lowercase().code() +
                (if (totalString != null) ", $totalString" else "")).trimStart(',', ' ') +
                System.lineSeparator() +
                statsString.asCodeBlock()

        if (requestResult.size > trimmedStringsList.size)
        {
            replyString += "\n  and ${requestResult.size - trimmedStringsList.size} more."
        }

        return replyString
    }
}

data class StatRequestResultRow(val playerName: String, val stat: StatData, val playtime: StatData)
{
    var rank: Int? = null
        private set

    fun setRank(rank: Int)
    {
        this.rank = rank
    }

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