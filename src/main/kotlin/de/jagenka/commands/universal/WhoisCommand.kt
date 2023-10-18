package de.jagenka.commands.universal

import de.jagenka.UserRegistry
import de.jagenka.UserRegistry.getPrettyUsersList
import de.jagenka.commands.StringInStringOutCommand
import kotlinx.coroutines.runBlocking


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

        return runBlocking { "Could be:\n\n" + possibleUsers.getPrettyUsersList() }
    }
}
