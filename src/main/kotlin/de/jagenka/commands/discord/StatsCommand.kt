package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.PlayerStatManager
import de.jagenka.Users
import de.jagenka.commands.discord.structure.Argument
import de.jagenka.commands.discord.structure.Argument.Companion.string
import de.jagenka.commands.discord.structure.ArgumentCombination
import de.jagenka.commands.discord.structure.MessageCommand
import net.minecraft.stat.Stat
import net.minecraft.stat.StatType
import net.minecraft.stat.Stats
import net.minecraft.util.Identifier
import java.util.*

object StatsCommand : MessageCommand
{
    override val ids: List<String>
        get() = listOf("stats")
    override val helpText: String
        get() = "Display Stats for players."
    override val allowedArgumentCombinations: List<ArgumentCombination>
        get() = listOf(
            ArgumentCombination(listOf(StatArgument(), string("stat")), "Get stat for all players.") { event, arguments ->
                println(Users.getAllKnownMinecraftUsers())

                val (argType, argText) = arguments[0]
                DiscordHandler.sendMessage(
                    Users.getAllKnownMinecraftUsers().map {
                        it.name to handle(
                            it.uuid,
                            (argType as StatArgument).convertToType(argText) ?: return@ArgumentCombination false,
                            arguments[1].second
                        )
                    }.filterNot { it.second == null }.sortedByDescending { it.second }.joinToString(separator = "\n") { "${it.first}: ${it.second}" }
                        .ifBlank { "Nothing found!".also { return@ArgumentCombination false } }
                )
                true
            },
            ArgumentCombination(listOf(string("playerName"), StatArgument(), string("stat")), "Get stat for one player.") { event, arguments ->
                val (_, playerName) = arguments.first()
                val (argType, argText) = arguments[1]
                DiscordHandler.sendMessage(
                    handle(
                        playerName,
                        (argType as StatArgument).convertToType(argText) ?: return@ArgumentCombination false,
                        arguments[2].second
                    )?.ifBlank { "Nothing found!" } ?: return@ArgumentCombination false
                )
                true
            },
        )

    private fun handle(playerName: String, statType: StatType<Any>, id: String): String?
    {
        return try
        {
            getReplyWithStat(playerName, statType.getOrCreateStat(statType.registry.get(Identifier(id))))
        } catch (_: Exception)
        {
            null
        }
    }

    private fun getReplyWithStat(playerName: String, stat: Stat<*>): String?
    {
        val statValue = PlayerStatManager.getStatHandlerForPlayer(playerName)?.getStat(stat)
        return statValue?.let { stat.format(it) }
    }

    private fun handle(uuid: UUID, statType: StatType<Any>, id: String): String?
    {
        return try
        {
            getReplyWithStat(uuid, statType.getOrCreateStat(statType.registry.get(Identifier(id))))
        } catch (_: Exception)
        {
            null
        }
    }

    private fun getReplyWithStat(uuid: UUID, stat: Stat<*>): String?
    {
        val statValue = PlayerStatManager.getStatHandlerForPlayer(uuid)?.getStat(stat)
        return statValue?.let { stat.format(it) }
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
        fun convert(word: String): StatType<Any>?
        {
            try
            {
                return when (word)
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
            } catch (_: Exception)
            {
                return null
            }
        }
    }
}