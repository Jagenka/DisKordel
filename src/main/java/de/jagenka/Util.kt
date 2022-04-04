package de.jagenka

import java.util.*

object Util
{
    fun Double.trim(digits: Int): String
    {
        return "%.${digits}f".format(this)
    }
    
    fun <T> Optional<T>.unwrap(): T? = orElse(null)
}