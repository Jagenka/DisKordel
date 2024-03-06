package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import de.jagenka.Main
import de.jagenka.UserRegistry
import de.jagenka.commands.DiskordelTextCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal
import dev.kord.core.entity.ReactionEmoji
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.launch

object UpdateNamesCommand : DiskordelTextCommand
{
    override val internalId: String
        get() = "updatenames"

    override val shortHelpText: String
        get() = "update Discord names in database"
    override val longHelpText: String
        get() = "this command is needed, if someone renames themselves in Discord. only with an update this mod will know of this change."

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        val commandNode = dispatcher.register(
            literal("updatenames")
                .executes {
                    Main.scope.launch {
                        UserRegistry.loadRegisteredUsersFromFile()
                        it.source.message.addReaction(ReactionEmoji.Unicode(Emojis.whiteCheckMark.unicode))
                    }
                    0
                }
        )

        Registry.registerShortHelpText(shortHelpText, commandNode)
        Registry.registerLongHelpText(longHelpText, commandNode)
    }
}