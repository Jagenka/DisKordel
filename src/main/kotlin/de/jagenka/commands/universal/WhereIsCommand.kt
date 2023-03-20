package de.jagenka.commands.universal

import de.jagenka.MinecraftHandler
import de.jagenka.UserRegistry

object WhereIsCommand : StringInStringOutCommand
{
    override val minecraftName: String
        get() = "whereis"
    override val ids: List<String>
        get() = listOf(minecraftName)
    override val helpText: String
        get() = "List where players are. No argument lists all players."
    override val variableName: String
        get() = "playerName"


    override fun process(input: String): String
    {
        val possibleUsers = UserRegistry.find(input.trim())

        if (possibleUsers.isEmpty()) return "No-one found!"

        return possibleUsers.joinToString("\n") { user ->
            MinecraftHandler.getPlayerPosition(user.minecraftName)?.let {
                "${user.minecraftName} is at (${it.x.toInt()} ${it.y.toInt()} ${it.z.toInt()})."
            } ?: ""
        }
    }
}