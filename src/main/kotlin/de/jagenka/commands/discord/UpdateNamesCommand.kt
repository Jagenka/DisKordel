package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.Main
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.x.emoji.Emojis
import kotlinx.coroutines.launch

object UpdateNamesCommand : DiscordCommand
{
    override val discordName: String
        get() = "updatenames"
    override val helpText: String
        get() = "`${DiscordCommandRegistry.commandPrefix}${discordName}`: Update Member names in registry."


    override fun execute(event: MessageCreateEvent, args: String)
    {
        Main.scope.launch {
            DiscordHandler.loadUsersFromFile()
            event.message.addReaction(ReactionEmoji.Unicode(Emojis.whiteCheckMark.unicode))
        }
    }
}