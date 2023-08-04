package de.jagenka.commands.universal

import de.jagenka.UserRegistry
import de.jagenka.UserRegistry.prettyString


object WhoisCommand : StringInStringOutCommand
{
    override val minecraftName: String
        get() = "whois"
    override val ids: List<String>
        get() = listOf(minecraftName)
    override val helpText: String
        get() = "List who some name might be. No argument lists all players."
    override val variableName: String
        get() = "playerName"


    override fun process(input: String): String
    {
        val possibleUsers = UserRegistry.findRegistered(input.trim())

        if (possibleUsers.isEmpty()) return "No-one found!"

        return possibleUsers.joinToString(prefix = "Could be:\n", separator = "\n") { it.prettyString() }
    }
}
