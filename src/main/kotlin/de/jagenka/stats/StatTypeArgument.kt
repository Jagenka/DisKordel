package de.jagenka.stats

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.minecraft.stat.StatType
import net.minecraft.stat.Stats

class StatTypeArgument : ArgumentType<StatType<*>>
{
    companion object
    {
        fun parse(input: String): StatType<*>?
        {
            return when (input)
            {
                "mined" -> Stats.MINED
                "crafted" -> Stats.CRAFTED
                "used" -> Stats.USED
                "broken" -> Stats.BROKEN
                "picked_up" -> Stats.PICKED_UP
                "dropped" -> Stats.DROPPED
                "killed" -> Stats.KILLED
                "killed_by" -> Stats.KILLED_BY
                "custom" -> Stats.CUSTOM
                else -> null
            }
        }
    }

    override fun parse(reader: StringReader?): StatType<*>
    {
        if (reader == null) throw NullPointerException("StringReader should not be null (yei Java Inter-Op)")

        return parse(reader.readUnquotedString()) ?: throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader)
    }

    override fun getExamples(): MutableCollection<String> = mutableListOf("mined", "crafted", "used", "broken", "picked_up", "dropped", "killed", "killed_by", "custom")
}