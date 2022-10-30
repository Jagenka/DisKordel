package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import dev.kord.core.event.message.MessageCreateEvent

object HelpCommand : DiscordCommand
{
    override val discordName: String
        get() = "help"
    override val helpText: String
        get() = "`${DiscordCommandRegistry.commandPrefix}${discordName}`: Shows this help text."


    override fun execute(event: MessageCreateEvent, args: String)
    {
        val stringBuilder = StringBuilder()

        DiscordCommandRegistry.getCommands().forEach { command ->
            stringBuilder.append(command.helpText)
            stringBuilder.appendLine()
        }

        DiscordHandler.sendMessage(stringBuilder.toString().trim())
    }
}