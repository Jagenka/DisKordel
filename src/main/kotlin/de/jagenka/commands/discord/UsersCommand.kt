package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.Users
import dev.kord.core.event.message.MessageCreateEvent

object UsersCommand : DiscordCommand
{
    override val discordName: String
        get() = "users"
    override val helpText: String
        get() = "`${DiscordCommandRegistry.commandPrefix}${discordName}`: Lists all registered users."


    override fun execute(event: MessageCreateEvent, args: String)
    {
        val sb = StringBuilder("Currently registered Users:")
        Users.getAsUserList().forEach {
            sb.appendLine()
            sb.append(it.prettyComboName)
        }
        DiscordHandler.sendMessage(sb.toString())
    }
}