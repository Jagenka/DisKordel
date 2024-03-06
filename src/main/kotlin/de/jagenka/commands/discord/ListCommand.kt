package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import de.jagenka.DiscordHandler
import de.jagenka.MinecraftHandler
import de.jagenka.commands.DiskordelTextCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal

object ListCommand : DiskordelTextCommand
{
    override val internalId: String
        get() = "list"

    override val shortHelpText: String
        get() = "list online players"
    override val longHelpText: String
        get() = "list players currently logged into the Minecraft server."

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        val commandNode = dispatcher.register(
            literal("list")
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

        Registry.registerShortHelpText(shortHelpText, commandNode)
        Registry.registerLongHelpText(longHelpText, commandNode)
    }
}