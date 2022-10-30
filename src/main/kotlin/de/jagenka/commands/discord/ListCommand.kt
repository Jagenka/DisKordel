package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.MinecraftHandler
import dev.kord.core.event.message.MessageCreateEvent

object ListCommand : DiscordCommand
{
    override val discordName: String
        get() = "list"
    override val helpText: String
        get() = "`${DiscordCommandRegistry.commandPrefix}${discordName}`: List Minecraft players currently in-game."


    override fun execute(event: MessageCreateEvent, args: String)
    {
        val onlinePlayers = MinecraftHandler.getOnlinePlayers()
        val sb = StringBuilder("Currently online: ")
        if (onlinePlayers.isEmpty()) sb.append("~nobody~, ")
        else onlinePlayers.forEach { sb.append("$it, ") }
        sb.deleteRange(sb.length - 2, sb.length)
        DiscordHandler.sendMessage(sb.toString())
    }
}