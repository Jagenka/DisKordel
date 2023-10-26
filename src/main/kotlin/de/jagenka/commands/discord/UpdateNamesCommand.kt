package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import de.jagenka.Main
import de.jagenka.UserRegistry
import de.jagenka.commands.DiscordCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal
import dev.kord.core.entity.ReactionEmoji
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.launch

object UpdateNamesCommand : DiscordCommand
{
    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        dispatcher.register(
            literal("updatenames")
                .executes {
                    Main.scope.launch {
                        UserRegistry.loadRegisteredUsersFromFile()
                        it.source.message.addReaction(ReactionEmoji.Unicode(Emojis.whiteCheckMark.unicode))
                    }
                    0
                }
        )
    }
}