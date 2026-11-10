package de.jagenka.commands.discord

import de.jagenka.MinecraftHandler
import de.jagenka.commands.DiskordelSlashCommand
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.RootInputChatBuilder

object ListCommand : DiskordelSlashCommand
{
    override val name: String
        get() = "list"
    override val description: String
        get() = "List players, who are currently connected to the Minecraft Server."

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
        val onlinePlayers = MinecraftHandler.getOnlinePlayers().map { it.name.string }
        val count = onlinePlayers.size
        val prefix =
            if (count == 1) "There is currently $count player connected to the Minecraft Server:\n"
            else "There are currently $count players connected to the Minecraft Server:\n"

        return if (count == 0)
        {
            "No players are currently connected to the Minecraft server."
        } else
        {
            onlinePlayers.joinToString("\n", prefix = prefix)
            {
                "- $it"
            }
        }
    }
}
