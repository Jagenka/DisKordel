package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import de.jagenka.DiscordHandler
import de.jagenka.commands.DiscordCommand
import de.jagenka.commands.universal.WhoisCommand
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on

object Registry
{
    private val messageCommandPrefix: String = "!"
    private val interactsWithBots: Boolean = true
    private val commandDispatcher = CommandDispatcher<MessageCommandSource>()

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
        it.member?.isOwner() == true
    }

    fun setup(kord: Kord)
    {
        registerCommands()

        kord.on<MessageCreateEvent> messageHandling@{
            if (message.getChannelOrNull()?.id != DiscordHandler.channel.id)
            {
                // return if message was not sent in set channel
                return@messageHandling
            }
            let commandHandling@{
                if (interactsWithBots)
                {
                    // return if author is self or undefined
                    if (message.author?.id == kord.selfId) return@commandHandling
                } else
                {
                    // return if author is a bot or undefined
                    if (message.author?.isBot != false) return@commandHandling
                }

                if (!message.content.startsWith(messageCommandPrefix)) return@commandHandling

                try
                {
                    commandDispatcher.execute(message.content.removePrefix(messageCommandPrefix), MessageCommandSource(this))
                    return@messageHandling
                } catch (e: CommandSyntaxException)
                {
                    return@commandHandling
                }
            }

            // if commandHandling fails, it must be a chat message, which is to be relayed to Minecraft
            DiscordHandler.relayChatMessage(messageCreateEvent = this)
        }
    }

    private fun registerCommands()
    {
        // Discord Commands
        register(TestCommand)

        // Universal Commands
        register(WhoisCommand)
    }

    private fun register(command: DiscordCommand)
    {
        command.registerWithDiscord(commandDispatcher)
    }

    // TODO: help texts / command
}