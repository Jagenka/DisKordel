package de.jagenka.commands.discord

import de.jagenka.MinecraftHandler
import de.jagenka.commands.DiskordelSlashCommand
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.RootInputChatBuilder

object PingCommand : DiskordelSlashCommand
{
    override val name: String
        get() = "ping"
    override val description: String
        get() = "Determine server ping to online Minecraft player(s)."

    override suspend fun build(builder: RootInputChatBuilder)
    {
        // nothing to declare - maybe add player filtering
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        with(event)
        {
            val response = interaction.deferEphemeralResponse()
            val onlinePlayers = MinecraftHandler.getOnlinePlayers()
            if (onlinePlayers.isEmpty())
            {
                response.respond { content = "No players online!" }
                return
            }

            val responseString = onlinePlayers.joinToString(
                separator = System.lineSeparator(),
                prefix = "Server ping to online player(s):${System.lineSeparator()}"
            ) { "- ${it.name.string}: ${it.networkHandler.latency} ms" }
            response.respond { content = responseString }
            return
        }
    }
}