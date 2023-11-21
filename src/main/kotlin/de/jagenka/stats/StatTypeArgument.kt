package de.jagenka.stats

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import net.minecraft.stat.StatType
import net.minecraft.stat.Stats

class StatTypeArgument : ArgumentType<StatType<*>>
{
    override fun parse(reader: StringReader?): StatType<*>
    {
        if (reader == null) throw NullPointerException("StringReader should not be null (yei Java Inter-Op)")

        return when (reader.readUnquotedString())
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
            else -> throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader)
        }
    }

    override fun getExamples(): MutableCollection<String> = mutableListOf("mined", "crafted", "used", "broken", "picked_up", "dropped", "killed", "killed_by", "custom")
}