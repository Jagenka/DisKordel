package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import dev.kord.core.event.message.MessageCreateEvent

object HelpCommand : DiscordCommand
{
    private const val helpString =
        "Available commands:\n" +
                "- `!register minecraftName`: connect your Minecraft name to your Discord account\n" +
                "- `!whitelist`: ensure that you're on the whitelist if it doesn't automatically work\n" +
                "\n" +
                "- `!users`: see all registered Users\n" +
                "- `!whois username`: look for a user\n" +
                "- `!updatenames`: update discord display names in database\n" +
                "\n" +
                "- `!list`: list currently online players\n" +
                "- `!deaths minecraftName`: shows how often a player has died\n" +
                "- `!playtime minecraftName`: shows a players playtime\n" +
                "\n" +
                "- `!help`: see this help text"

    override val discordName: String
        get() = "help"

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