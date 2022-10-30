package de.jagenka.commands.discord

import dev.kord.core.event.message.MessageCreateEvent

interface DiscordCommand
{
    val discordName: String

    val helpText: String
        get() = "`$discordName` is missing a help text."

    val needsAdmin: Boolean
        get() = false

    fun execute(event: MessageCreateEvent, args: String)
}