package de.jagenka.commands.universal

import de.jagenka.Users


object WhoisCommand : StringInStringOutCommand
{
    override val minecraftName: String
        get() = "whois"
    override val discordName: String
        get() = minecraftName

    override fun process(input: String): String
    {
        return Users.whoIsPrintable(input.trim())
    }
}
