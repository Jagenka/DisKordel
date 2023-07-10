package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.UserRegistry
import de.jagenka.commands.discord.structure.Argument
import de.jagenka.commands.discord.structure.ArgumentCombination
import de.jagenka.commands.discord.structure.MessageCommand

object RelativeStatsCommand : MessageCommand
{
    override val ids: List<String>
        get() = listOf("relativestats", "rstats")
    override val helpText: String
        get() = "display non-zero stats in relation to the player's playtime"
    override val allowedArgumentCombinations: List<ArgumentCombination>
        get() = listOf(
            ArgumentCombination(listOf(StatArgument(), Argument.string("stat")), "Get stat for all players.") { event, arguments ->
                val (argType, argText) = arguments[0]
                DiscordHandler.sendMessage(
                    StatsCommand.getRelativeReplyForAll(
                        (argType as StatArgument).convertToType(argText) ?: return@ArgumentCombination false,
                        arguments[1].second
                    )
                )
                true
            },
            ArgumentCombination(listOf(StatArgument(), Argument.string("stat"), Argument.string("partOfName")), "Get stat for some players.") { event, arguments ->
                val (_, playerName) = arguments[2]
                val (argType, argText) = arguments[0]
                DiscordHandler.sendMessage(
                    StatsCommand.getRelativeReplyForSome(
                        UserRegistry.findMinecraftProfiles(playerName),
                        (argType as StatArgument).convertToType(argText) ?: return@ArgumentCombination false,
                        arguments[1].second
                    )
                )
                true
            },
        )
}