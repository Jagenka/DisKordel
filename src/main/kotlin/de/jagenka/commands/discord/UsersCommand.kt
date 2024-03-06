package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import de.jagenka.DiscordHandler
import de.jagenka.Main
import de.jagenka.UserRegistry
import de.jagenka.commands.DiskordelTextCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal
import kotlinx.coroutines.launch

object UsersCommand : DiskordelTextCommand
{
    override val internalId: String
        get() = "users"

    override val shortHelpText: String
        get() = "list all registered users"
    override val longHelpText: String
        get() = "list all registered users."

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        val commandNode = dispatcher.register(literal("users")
            .executes {
                Main.scope.launch {
                    DiscordHandler.sendCodeBlock(text = UserRegistry.getAllUsersAsOutput(), silent = true)
                }
                0
            }
        )

        Registry.registerShortHelpText(shortHelpText, commandNode)
        Registry.registerLongHelpText(longHelpText, commandNode)
    }
}