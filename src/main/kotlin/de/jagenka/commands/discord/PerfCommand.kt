package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.MinecraftHandler
import de.jagenka.Util.trim
import dev.kord.core.event.message.MessageCreateEvent

object PerfCommand : DiscordCommand
{
    override val discordName: String
        get() = "perf"

    override fun execute(event: MessageCreateEvent, args: String)
    {
        val performanceMetrics = MinecraftHandler.getPerformanceMetrics()
        DiscordHandler.sendMessage("TPS: ${performanceMetrics.tps.trim(1)} MSPT: ${performanceMetrics.mspt.trim(1)}")
    }
}