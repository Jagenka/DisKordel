package de.jagenka.stats

data class RStatData(val playerName: String, val stat: Int, val playtime: Int)
{
    val relStat: Double
        get() = (if (playtime == 0) 0.0 else (stat.toDouble() * 72_000.0)) / playtime.toDouble() // converts ticks to hours (20*60*60)
}