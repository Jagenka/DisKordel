package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.PlayerStatManager
import dev.kord.core.event.message.MessageCreateEvent
import net.minecraft.registry.Registries
import net.minecraft.stat.Stats
import net.minecraft.util.Identifier

object StatsCommand : DiscordCommand
{
    override val discordName: String
        get() = "stats"
    override val helpText: String
        get() = "Display Stats for players."

    override fun execute(event: MessageCreateEvent, args: String)
    {
        val (playerName, stat) = args.trim().split(" ")

        DiscordHandler.sendMessage(
            PlayerStatManager.getStatHandlerForPlayer(playerName)
                ?.getStat(Stats.CUSTOM.getOrCreateStat(Registries.CUSTOM_STAT.get(Identifier(stat))))?.toString() ?: "null"
        )
    }
}