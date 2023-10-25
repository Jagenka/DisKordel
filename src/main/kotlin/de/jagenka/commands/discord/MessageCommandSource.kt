package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent

class MessageCommandSource(private val event: MessageCreateEvent)
{
    fun sendFeedback(text: String)
    {
        DiscordHandler.sendCodeBlock(text = text)
    }

    val author: User?
        get() = event.message.author
}