package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.PlayerStatManager
import de.jagenka.commands.discord.structure.Argument
import de.jagenka.commands.discord.structure.Argument.Companion.string
import de.jagenka.commands.discord.structure.ArgumentCombination
import de.jagenka.commands.discord.structure.MessageCommand
import net.minecraft.stat.Stat
import net.minecraft.stat.StatType
import net.minecraft.stat.Stats
import net.minecraft.util.Identifier

object StatsCommand : MessageCommand
{
    override val ids: List<String>
        get() = listOf("stats")
    override val helpText: String
        get() = "Display Stats for players."
    override val allowedArgumentCombinations: List<ArgumentCombination>
        get() = listOf(
            ArgumentCombination(listOf(string("playerName"), StatArgument(), string("stat")), "Get stat related to any for one player.") { event, arguments ->
                val (_, playerName) = arguments.first()
                val (argType, argText) = arguments[1]
                return@ArgumentCombination handle(
                    playerName,
                    (argType as StatArgument).convertToType(argText) ?: return@ArgumentCombination false,
                    arguments[2].second
                )
            },
        )

    private fun handleIdentifierStatType(playerName: String, id: String): Boolean
    {
        val stat = Stats.CUSTOM.getOrCreateStat(Stats.CUSTOM.registry.get(Identifier(id)))
        return replyWithStat(playerName, stat)
    }

    private fun handle(playerName: String, statType: StatType<Any>, id: String): Boolean // TODO: catch errors
    {
        val stat = statType.getOrCreateStat(statType.registry.get(Identifier(id)))
        return replyWithStat(playerName, stat)
    }

    private fun replyWithStat(playerName: String, stat: Stat<*>): Boolean
    {
        val statValue = PlayerStatManager.getStatHandlerForPlayer(playerName)
            ?.getStat(stat)

        DiscordHandler.sendMessage(
            statValue?.let { stat.format(it) } ?: "No stat found!".also { return false } // TODO: better formatting than Vanilla
        )
        return true
    }
}

class StatArgument : Argument<StatType<Any>>
{
    private val literals = listOf("mined", "crafted", "used", "broken", "picked_up", "dropped", "killed", "killed_by", "custom")

    override val id: String
        get() = literals.joinToString("|")

    override fun isOfType(word: String): Boolean
    {
        return word in literals
    }

    override fun convertToType(word: String): StatType<Any>?
    {
        return convert(word)
    }

    companion object
    {
        fun convert(word: String): StatType<Any>? = when (word)
        {
            "mined" -> Stats.MINED as StatType<Any>
            "crafted" -> Stats.CRAFTED as StatType<Any>
            "used" -> Stats.USED as StatType<Any>
            "broken" -> Stats.BROKEN as StatType<Any>
            "picked_up" -> Stats.PICKED_UP as StatType<Any>
            "dropped" -> Stats.DROPPED as StatType<Any>
            "killed" -> Stats.KILLED as StatType<Any>
            "killed_by" -> Stats.KILLED_BY as StatType<Any>
            "custom" -> Stats.CUSTOM as StatType<Any>
            else -> null
        }
    }
}