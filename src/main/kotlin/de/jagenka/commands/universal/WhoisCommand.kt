package de.jagenka.commands.universal

import de.jagenka.Users
import de.jagenka.commands.discord.DiscordCommandRegistry


object WhoisCommand : StringInStringOutCommand
{
    override val minecraftName: String
        get() = "whois"
    override val discordName: String
        get() = minecraftName
    override val helpText: String
        get() = "`${DiscordCommandRegistry.commandPrefix}${discordName} [name]`: List who some name might be. No argument lists all players."


    override fun process(input: String): String
    {
        return Users.whoIsPrintable(input.trim())
    }
}
