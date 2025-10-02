package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import de.jagenka.DiscordHandler
import de.jagenka.DiscordHandler.prettyName
import de.jagenka.Main
import de.jagenka.MinecraftHandler
import de.jagenka.UserRegistry
import de.jagenka.commands.DiskordelSlashCommand
import de.jagenka.commands.DiskordelTextCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.RootInputChatBuilder
import kotlinx.coroutines.launch

object UnregisterCommand : DiskordelTextCommand, DiskordelSlashCommand
{
    override val name: String
        get() = "unregister"
    override val description: String
        get() = "Remove Discord-Minecraft link. This will also remove your name from the whitelist."

    override suspend fun build(builder: RootInputChatBuilder)
    {
        // nothing to declare
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        val discordId = event.interaction.user.id
        val response = unregisterUserWithResponse(discordId)
        event.interaction.respondEphemeral {
            content = response
        }
    }

    /**
     * @return response string
     */
    suspend fun unregisterUserWithResponse(userId: Snowflake): String
    {
        val member = DiscordHandler.getMemberOrSendError(userId) ?: return ""

        var response = ""

        UserRegistry.findUser(userId)?.let { oldUser ->
            MinecraftHandler.runWhitelistRemove(oldUser.minecraft.username)
            response += "`${oldUser.minecraft.username}` is no longer whitelisted.\n"
        }

        response +=
            if (UserRegistry.unregister(userId))
            {
                "${member.prettyName()} now unregistered."
            } else
            {
                "${member.prettyName()} was not registered."
            }

        UserRegistry.saveToFile()

        return response
    }

    fun unregisterUser(userId: Snowflake)
    {
        UserRegistry.findUser(userId)?.let { oldUser ->
            MinecraftHandler.runWhitelistRemove(oldUser.minecraft.username)
        }
        UserRegistry.unregister(userId)
        UserRegistry.saveToFile()
    }

    override val shortHelpText: String
        get() = "remove Discord-Minecraft link"
    override val longHelpText: String
        get() = "remove link between your Discord account and the saved Minecraft name. this will also remove you from the whitelist."

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        val commandNode = dispatcher.register(
            literal("unregister")
                .executes {
                    it.source.author?.let { user ->
                        Main.scope.launch {
                            val response = unregisterUserWithResponse(user.id)
                            DiscordHandler.sendMessage(response, silent = true)
                        }
                    }
                    0
                }
        )

        Registry.registerShortHelpText(shortHelpText, commandNode)
        Registry.registerLongHelpText(longHelpText, commandNode)
    }
}