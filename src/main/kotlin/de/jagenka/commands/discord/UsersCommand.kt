package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import de.jagenka.DiscordHandler
import de.jagenka.DiscordHandler.asCodeBlock
import de.jagenka.Main
import de.jagenka.UserRegistry
import de.jagenka.commands.DiskordelSlashCommand
import de.jagenka.commands.DiskordelTextCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.RootInputChatBuilder
import kotlinx.coroutines.launch

object UsersCommand : DiskordelTextCommand, DiskordelSlashCommand
{
    override val name: String
        get() = "users"
    override val description: String
        get() = "List all registered users."

    override suspend fun build(builder: RootInputChatBuilder)
    {
        // nothing to declare
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        event.interaction.respondEphemeral {
            content = UserRegistry.getAllUsersAsOutput().asCodeBlock()
        }
    }

    override val shortHelpText: String
        get() = "list all registered users"
    override val longHelpText: String
        get() = "list all registered users."

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        val commandNode = dispatcher.register(literal("users")
            .executes {
                Main.scope.launch {
                    DiscordHandler.sendCodeBlock(text = UserRegistry.getAllUsersAsOutput(), silent = true)
                }
                0
            }
        )

        Registry.registerShortHelpText(shortHelpText, commandNode)
        Registry.registerLongHelpText(longHelpText, commandNode)
    }
}