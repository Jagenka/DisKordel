package de.jagenka.commands.discord

import dev.kord.core.event.message.MessageCreateEvent

interface DiscordCommand
{
    val discordName: String

    fun execute(event: MessageCreateEvent, args: String)
}