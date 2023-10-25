package de.jagenka.commands.universal

import de.jagenka.MinecraftHandler
import de.jagenka.UserRegistry
import de.jagenka.commands.StringInStringOutCommand

object WhereIsCommand : StringInStringOutCommand("whereis")
{
    override fun process(input: String): String
    {
        val possibleUsers = UserRegistry.findRegistered(input.trim())

        if (possibleUsers.isEmpty()) return "No-one found!"

        return possibleUsers.joinToString("\n") { user ->
            MinecraftHandler.getPlayerPosition(user.minecraft.name)?.let {
                "${user.minecraft.name} is at (${it.x.toInt()} ${it.y.toInt()} ${it.z.toInt()})."
            } ?: "No-one found!"
        }
    }
}