package de.jagenka.commands.discord

import de.jagenka.MinecraftHandler
import de.jagenka.Util.trimDecimals
import de.jagenka.commands.DiskordelSlashCommand
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.RootInputChatBuilder

object PerfCommand : DiskordelSlashCommand
{
    override val name: String
        get() = "performance"
    override val description: String
        get() = "Query current server ticks per second (TPS) and milliseconds per tick (MSPT)."

    override suspend fun build(builder: RootInputChatBuilder)
    {
        // nothing needed
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        event.interaction.respondEphemeral {
            content = getResponse()
        }
    }

    private fun getResponse(): String
    {
        val performanceMetrics = MinecraftHandler.getPerformanceMetrics()
        return "TPS: ${performanceMetrics.tps.toDouble().trimDecimals(1)} MSPT: ${performanceMetrics.mspt.trimDecimals(1)}"
    }
}
