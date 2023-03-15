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
            ArgumentCombination(listOf(string("playerName"), string("stat")), helpText) { event, arguments ->
                val playerName = arguments["playerName"].toString()
                val stat = arguments["stat"].toString()

                DiscordHandler.sendMessage(
                    PlayerStatManager.getStatHandlerForPlayer(playerName)
                        ?.getStat(Stats.CUSTOM.getOrCreateStat(Registries.CUSTOM_STAT.get(Identifier(stat))))?.toString() ?: "null"
                )

                true
            })
}