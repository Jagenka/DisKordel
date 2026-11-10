package de.jagenka.commands.discord

import de.jagenka.commands.DiskordelSlashCommand
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.RootInputChatBuilder

/**
 * Rather than duplicating command documentation here (and risking it drifting out of sync with the manual on
 * GitHub, like the old in-Discord help text eventually did), this just points to the manual.
 */
object HelpCommand : DiskordelSlashCommand
{
    override val name: String
        get() = "help"
    override val description: String
        get() = "Get a link to the command documentation."

    override suspend fun build(builder: RootInputChatBuilder)
    {
        // nothing needed
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        event.interaction.respondEphemeral {
            content = "See the manual on GitHub for a full overview of commands:\n" +
                    "- https://github.com/Jagenka/DisKordel/blob/master/manual/commands.md\n" +
                    "- https://github.com/Jagenka/DisKordel/blob/master/manual/queryable_stats.md (for `/stat` and `/speed`)"
        }
    }
}
