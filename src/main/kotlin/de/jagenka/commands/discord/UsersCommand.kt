package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import de.jagenka.DiscordHandler
import de.jagenka.Main
import de.jagenka.UserRegistry
import de.jagenka.commands.DiscordCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal
import kotlinx.coroutines.launch

object UsersCommand : DiscordCommand
{
    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        dispatcher.register(literal("users")
            .executes {
                Main.scope.launch {
                    DiscordHandler.sendCodeBlock(text = UserRegistry.getAllUsersAsOutput(), silent = true)
                }
                0
            })
    }
}