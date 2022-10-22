package de.jagenka.commands

import dev.kord.core.event.message.MessageCreateEvent

object DiscordCommandRegistry
{
    private const val commandPrefix = "!"

    private val commands = mutableSetOf<DiscordCommand>()

    fun register(command: DiscordCommand)
    {
        commands.add(command)
    }

    fun handleCommand(event: MessageCreateEvent)
    {
        val messageContent = event.message.content

        println(messageContent)

        messageContent.split(" ").firstOrNull()?.let { first ->
            println(first)
            if (first.startsWith(commandPrefix))
            {
                val commandLiteral = first.removePrefix(commandPrefix)
                println(commandLiteral)
                commands.find { it.discordName == commandLiteral }?.execute(event, messageContent.removePrefix(first).trim())
            }
        }
    }
}