package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import de.jagenka.DiscordHandler
import de.jagenka.MinecraftHandler.logger
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

                val command = message.content.removePrefix(messageCommandPrefix)

                try
                {
                    commandDispatcher.execute(command, MessageCommandSource(event = this))
                    return@messageHandling
                } catch (e: CommandSyntaxException)
                {
                    // TODO: give feedback
                    logger.error(e.rawMessage.string)
                    logger.error(e.input)

                    return@commandHandling
                }
            }

            // if commandHandling fails, it must be a chat message, which is to be relayed to Minecraft
            DiscordHandler.relayChatMessage(messageCreateEvent = this)
        }
    }

    private fun registerCommands()
    {
        listOf(
            // Discord Commands
            PerfCommand,
            ListCommand,
            UsersCommand,
            UpdateNamesCommand,
            RegisterCommand,
            UnregisterCommand,
            StatsCommand,
            RelativeStatsCommand,

            // Universal Commands
            WhoisCommand,
        ).forEach { it.registerWithDiscord(commandDispatcher) }
    }

    // TODO: help texts / command
}