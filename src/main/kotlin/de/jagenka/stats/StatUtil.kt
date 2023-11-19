@file:Suppress("UNCHECKED_CAST")

package de.jagenka.stats

import de.jagenka.StatDataException
import de.jagenka.StatDataExceptionType.*
import de.jagenka.UserRegistry
import net.minecraft.stat.StatType
import net.minecraft.stat.Stats
import net.minecraft.util.Identifier

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

    /**
     * @return sorted list of 1-indexed rank to StatData. rank is grouped if stats are equal
     */
    @Throws(StatDataException::class)
    fun getStatDataWithRanks(statType: StatType<Any>, id: String): List<Pair<Int, StatData>>
    {
        val data = getStatDataList(statType, id)
        if (data.isEmpty()) throw StatDataException(EMPTY)
        if (data.all { it.value == 0 }) throw StatDataException(ONLY_ZERO)

        var currentRank = 1
        var currentValue: Int? = null

        return data
            .sortedByDescending { it.value }
            .mapIndexed { index, statData ->
                if (statData.value != currentValue)
                {
                    currentRank = index + 1
                    currentValue = statData.value
                }

                currentRank to statData
            }
    }

    /**
     * @return sorted list of 1-indexed rank to StatData to PlaytimeData. rank is grouped if stats are equal
     */
    @Throws(StatDataException::class)
    fun getRelativeStatDataWithRank(statType: StatType<Any>, id: String): List<Triple<Int, StatData, StatData>>
    {
        val data = getStatDataList(statType, id)
        if (data.isEmpty()) throw StatDataException(EMPTY)
        if (data.all { it.value == 0 }) throw StatDataException(ONLY_ZERO)

        val playtimeData = StatUtil.getStatDataList(Stats.CUSTOM as StatType<Any>, "play_time")
        if (playtimeData.isEmpty()) throw StatDataException(EMPTY)
        if (playtimeData.all { it.value == 0 }) throw StatDataException(ONLY_ZERO)

        var currentRank = 1
        var currentValue: Double? = null

        return data
            .mapNotNull {
                it to (playtimeData.find { playtimeData ->
                    playtimeData.playerName.equals(it.playerName, ignoreCase = true)
                } ?: return@mapNotNull null)
            }
            .sortedByDescending { (statData, playtimeData) ->
                getRelStat(statData.value, playtimeData.value)
            }
            .mapIndexed { index, (statData, playtimeData) ->
                val relStat = getRelStat(statData.value, playtimeData.value)
                if (relStat != currentValue)
                {
                    currentRank = index + 1
                    currentValue = relStat
                }

                Triple(currentRank, statData, playtimeData)
            }
    }

    fun getRelStat(stat: Int, playtime: Int): Double
    {
        return (if (playtime == 0) 0.0 else (stat.toDouble() * 72_000.0)) / playtime.toDouble() // converts ticks to hours (20*60*60)
    }
}