package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.MinecraftHandler
import de.jagenka.commands.discord.structure.ArgumentCombination
import de.jagenka.commands.discord.structure.ArgumentCombination.Companion.empty
import de.jagenka.commands.discord.structure.MessageCommand

object ListCommand : MessageCommand
{
    override val ids: List<String>
        get() = listOf("list")
    override val helpText: String
        get() = "List Minecraft players currently in-game."

    override val allowedArgumentCombinations: List<ArgumentCombination>
        get() = listOf(empty(helpText) { _ ->
            val onlinePlayers = MinecraftHandler.getOnlinePlayers()
            val sb = StringBuilder("Currently online: ")
            if (onlinePlayers.isEmpty()) sb.append("~nobody~, ")
            else onlinePlayers.forEach { sb.append("$it, ") }
            sb.deleteRange(sb.length - 2, sb.length)
            DiscordHandler.sendMessage(sb.toString())
            true
        })
}