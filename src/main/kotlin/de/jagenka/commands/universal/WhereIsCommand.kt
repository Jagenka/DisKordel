package de.jagenka.commands.universal

import de.jagenka.MinecraftHandler
import de.jagenka.Users
import de.jagenka.commands.discord.DiscordCommandRegistry

object WhereIsCommand : StringInStringOutCommand
{
    override val minecraftName: String
        get() = "whereis"
    override val discordName: String
        get() = minecraftName
    override val helpText: String
        get() = "`${DiscordCommandRegistry.commandPrefix}${discordName} [name]`: List where players are. No argument lists all players."


    override fun process(input: String): String
    {
        val possibleUsers = Users.find(input.trim())

        if (possibleUsers.isEmpty()) return "No-one found!"

        return possibleUsers.joinToString("\n") { user ->
            MinecraftHandler.getPlayerPosition(user.minecraftName)?.let {
                "${user.minecraftName} is at (${it.x.toInt()} ${it.y.toInt()} ${it.z.toInt()})."
            } ?: ""
        }
    }
}