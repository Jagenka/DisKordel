package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import de.jagenka.DiscordHandler
import de.jagenka.MinecraftHandler
import de.jagenka.commands.DiskordelSlashCommand
import de.jagenka.commands.DiskordelTextCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.RootInputChatBuilder

object ListCommand : DiskordelTextCommand, DiskordelSlashCommand
{
    override val internalId: String
        get() = "list"

    override val name: String
        get() = "list"
    override val description: String
        get() = "List players, who are currently connected to the Minecraft Server."

    override suspend fun build(builder: RootInputChatBuilder)
    {
        // nothing needed
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        event.interaction.respondEphemeral {
            content = getResponse()
        }
    }

    override val shortHelpText: String
        get() = "list online players"
    override val longHelpText: String
        get() = "list players currently logged into the Minecraft server."

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        val commandNode = dispatcher.register(
            literal("list")
                .executes {
                    DiscordHandler.sendMessage(text = getResponse(), silent = true)
                    0
                }
        )

        Registry.registerShortHelpText(shortHelpText, commandNode)
        Registry.registerLongHelpText(longHelpText, commandNode)
    }

    private fun getResponse(): String
    {
        val onlinePlayers = MinecraftHandler.getOnlinePlayers()
        return "Currently online:\n" +
                if (onlinePlayers.isEmpty()) "~nobody~"
                else
                {
                    onlinePlayers.joinToString(separator = "\n") {
                        "- $it"
                    }
                }
    }
}