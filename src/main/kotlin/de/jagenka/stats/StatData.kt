package de.jagenka.stats

import net.minecraft.stat.StatFormatter

data class StatData(val id: String, val playerName: String, val value: Int, val type: StatDataType, val formatter: StatFormatter? = null)

enum class StatDataType
{
    NONE, TIME, DISTANCE;

    companion object Companion
    {
        fun fromFormatter(formatter: StatFormatter): StatDataType
        {
            return when (formatter)
            {
                StatFormatter.TIME -> TIME
                StatFormatter.DISTANCE -> DISTANCE
                else -> NONE
            }
        }
    }
}
