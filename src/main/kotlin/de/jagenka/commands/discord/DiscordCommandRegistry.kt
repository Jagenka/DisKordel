package de.jagenka.commands.discord

import de.jagenka.MinecraftHandler
import dev.kord.core.event.message.MessageCreateEvent

object DiscordCommandRegistry
{
    const val commandPrefix = "!"

    private val commands = mutableListOf<DiscordCommand>()

    fun register(command: DiscordCommand)
    {
        commands.add(command)
    }

    fun handleCommand(event: MessageCreateEvent)
    {
        val messageContent = event.message.content

        if (messageContent.startsWith(commandPrefix))
        {
            messageContent.split(" ").firstOrNull()?.let { first ->
                val commandLiteral = first.removePrefix(commandPrefix)
                commands.find { it.discordName == commandLiteral }?.execute(event, messageContent.removePrefix(first).trim())
            }
        } else
        {
            defaultHandling(event)
        }
    }

    private fun defaultHandling(event: MessageCreateEvent)
    {
        val authorName = event.member?.displayName ?: event.message.author?.username ?: "NONAME"
        MinecraftHandler.sendMessage(authorName, event.message.content)
    }

    fun getCommands(): List<DiscordCommand> = commands.toList().sortedBy { it.discordName }
}