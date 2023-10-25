package de.jagenka.commands.universal

import de.jagenka.UserRegistry
import de.jagenka.UserRegistry.getPrettyUsersList
import de.jagenka.commands.StringInStringOutCommand
import kotlinx.coroutines.runBlocking


object WhoisCommand : StringInStringOutCommand("whois")
{
    override fun process(input: String): String
    {
        val possibleUsers = UserRegistry.findRegistered(input.trim())

        if (possibleUsers.isEmpty()) return "No-one found!"

        return runBlocking { "Could be:\n\n" + possibleUsers.getPrettyUsersList() }
    }
}
