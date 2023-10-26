package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import de.jagenka.DiscordHandler
import de.jagenka.MinecraftHandler
import de.jagenka.commands.DiscordCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal

object ListCommand : DiscordCommand
{
    private const val NAME = "list"

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        dispatcher.register(
            literal(NAME)
                .executes {
                    val onlinePlayers = MinecraftHandler.getOnlinePlayers()
                    DiscordHandler.sendMessage(
                        text = "Currently online:\n" +
                                if (onlinePlayers.isEmpty()) "~nobody~"
                                else
                                {
                                    onlinePlayers.joinToString(separator = "\n") {
                                        "- $it"
                                    }
                                }, silent = true)
                    0
                }
        )
    }
}