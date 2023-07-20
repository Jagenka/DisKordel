package de.jagenka

import java.util.*

object Util
{
    fun Double.trimDecimals(digits: Int): String
    {
        return "%.${digits}f".format(Locale.US, this)
    }

    fun <T> Optional<T>.unwrap(): T? = orElse(null)

    fun ticksToPrettyString(ticks: Int): String
    {
        val seconds = ticks / 20
        val minutes = seconds / 60
        val hours = minutes / 60

        val sb = StringBuilder()
        if (hours > 0) sb.append("${hours}h")
        if (hours > 0 || minutes > 0) sb.append(" ${minutes - hours * 60}min")
        if (hours > 0 || minutes > 0 || seconds > 0) sb.append(" ${seconds - minutes * 60}s")
        else sb.append("0h 0min 0s")

        return sb.toString().trim()
    }
}