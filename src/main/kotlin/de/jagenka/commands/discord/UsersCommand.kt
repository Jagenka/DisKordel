package de.jagenka.commands.discord

import de.jagenka.UserRegistry
import de.jagenka.commands.DiskordelSlashCommand
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.RootInputChatBuilder

object UsersCommand : DiskordelSlashCommand
{
    override val name: String
        get() = "users"
    override val description: String
        get() = "List all registered users."

    override suspend fun build(builder: RootInputChatBuilder)
    {
        // nothing to declare
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        event.interaction.respondEphemeral {
            content = UserRegistry.getAllUsersAsOutput()
        }
    }
}
