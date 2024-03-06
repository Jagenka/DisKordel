package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.DiscordHandler
import de.jagenka.commands.DiskordelTextCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.argument
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal
import de.jagenka.commands.discord.MessageCommandSource.Companion.redirect

object HelpCommand : DiskordelTextCommand
{
    override val internalId: String
        get() = "help"

    override val shortHelpText: String
        get() = "help with commands"
    override val longHelpText: String
        get() = "get command overview, or help for specific command."

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        val commandNode = dispatcher.register(
            literal("help")
                .executes {
                    DiscordHandler.sendMessage(
                        text = Registry.getShortHelpTexts(it.source).joinToString(separator = System.lineSeparator()),
                        silent = true
                    )
                    0
                }
                .then(argument("command", StringArgumentType.string())
                    .executes {
                        DiscordHandler.sendMessage(
                            text = Registry.getHelpTextsForCommand(it.source, StringArgumentType.getString(it, "command")).joinToString(separator = System.lineSeparator()),
                            silent = true
                        )
                        0
                    }
                )
        )

        val redirect = dispatcher.register(redirect("?", commandNode))

        Registry.registerShortHelpText(shortHelpText, commandNode, redirect)
        Registry.registerLongHelpText(longHelpText, commandNode, redirect)
    }
}