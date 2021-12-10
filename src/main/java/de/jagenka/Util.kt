package de.jagenka

object Util
{
    fun Double.trim(digits: Int): String
    {
        return "%.${digits}f".format(this)
    }
}