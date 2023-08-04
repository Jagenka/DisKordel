package de.jagenka.commands.discord.structure

import de.jagenka.DiscordHandler
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on

object Registry
{
    private val messageCommandPrefix: String = "!"
    private val interactsWithBots: Boolean = true
    internal val commands = mutableMapOf<String, MessageCommand>()

    /**
     * suspend function to be called if a command needs admin, but the sender does not have the admin role.
     */
    var needsAdminResponse: suspend (event: MessageCreateEvent) -> Unit = {
        it.message.channel.createMessage("You need to be admin to do that!")
    }

    /**
     * suspend function to be called if a command can only execute in a channel marked "NSFW", but isn't marked as such.
     */
    var needsNSFWResponse: suspend (event: MessageCreateEvent) -> Unit = {
        it.message.channel.createMessage("You can only do that in a channel marked \"NSFW\"!")
    }

    var isSenderAdmin: suspend (event: MessageCreateEvent) -> Boolean = {
        it.member?.isOwner() == true // TODO: adminRole
    }

    fun setup(kord: Kord)
    {
        kord.on<MessageCreateEvent> {
            if (message.getChannelOrNull()?.id != DiscordHandler.channel.id)
            {
                // return if message was not sent in set channel
                return@on
            }
            let {
                if (interactsWithBots)
                {
                    // return if author is self or undefined
                    if (message.author?.id == kord.selfId) return@let
                } else
                {
                    // return if author is a bot or undefined
                    if (message.author?.isBot != false) return@let
                }

                if (!message.content.startsWith(messageCommandPrefix)) return@let

                val args = this.message.content.split(" ")
                val firstWord = args.getOrNull(0) ?: return@let

                val command = commands[firstWord.removePrefix(messageCommandPrefix)] ?: return@let

                if (command.run(this, args)) return@on
            }

            DiscordHandler.handleNotACommand(this)
        }
    }

    /**
     * Register your own implementation of a MessageCommand here. This method will also call `command.prepare(kord)`.
     * @param command is said custom implementation of MessageCommand.
     */
    fun register(command: MessageCommand)
    {
        command.ids.forEach {
            if (commands.containsKey(it)) error("command id `$it` is already assigned to command ${commands[it]}")
            commands[it] = command
        }

        command.prepare(this)
    }

    internal suspend fun getShortHelpTexts(event: MessageCreateEvent): List<String>
    {
        return commands.values.toSortedSet().filter { isSenderAdmin.invoke(event) || it.allowedArgumentCombinations.any { !it.needsAdmin } }
            .map { it.ids.joinToString(separator = "|", postfix = ": ${it.helpText}") { "`$messageCommandPrefix$it`" } }
    }

    internal suspend fun getHelpTextsForCommand(id: String, event: MessageCreateEvent): List<String>
    {
        return commands[id]?.allowedArgumentCombinations?.filter { !it.needsAdmin || isSenderAdmin.invoke(event) }?.map {
            it.arguments.joinToString(prefix = "`$messageCommandPrefix$id ", separator = " ") {
                it.displayInHelp
            }.trim() + "`: ${it.helpText}"
        } ?: listOf("`$messageCommandPrefix$id` is not a valid command.")
    }
}