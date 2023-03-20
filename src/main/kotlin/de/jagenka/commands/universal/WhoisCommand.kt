package de.jagenka.commands.universal

import de.jagenka.UserRegistry


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
        return UserRegistry.whoIsPrintable(input.trim())
    }
}
