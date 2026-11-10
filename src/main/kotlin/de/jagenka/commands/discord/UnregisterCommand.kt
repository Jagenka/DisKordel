package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.DiscordHandler.prettyName
import de.jagenka.MinecraftHandler
import de.jagenka.UserRegistry
import de.jagenka.commands.DiskordelSlashCommand
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.RootInputChatBuilder

object UnregisterCommand : DiskordelSlashCommand
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
}
