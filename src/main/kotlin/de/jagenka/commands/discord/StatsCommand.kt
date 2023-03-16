package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.PlayerStatManager
import de.jagenka.commands.discord.structure.Argument.Companion.string
import de.jagenka.commands.discord.structure.ArgumentCombination
import de.jagenka.commands.discord.structure.MessageCommand
import net.minecraft.registry.Registries
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
            ArgumentCombination(listOf(string("playerName"), string("stat")), "Given an identifier, show a players stat value.") { event, arguments ->
                val playerName = arguments["playerName"].toString()
                val stat = arguments["stat"].toString() // TODO: other identifiers?

                val statObject = Stats.CUSTOM.getOrCreateStat(Registries.CUSTOM_STAT.get(Identifier(stat))) // TODO: other stat types than Stats.CUSTOM
                val statValue = PlayerStatManager.getStatHandlerForPlayer(playerName)
                    ?.getStat(statObject)

                DiscordHandler.sendMessage(
                    statValue?.let { statObject.format(it) } ?: "No stat found!" // TODO: better formatting than Vanilla
                )

                true
            })
}