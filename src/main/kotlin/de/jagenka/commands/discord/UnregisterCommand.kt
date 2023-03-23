package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.DiscordHandler.prettyName
import de.jagenka.MinecraftHandler
import de.jagenka.UserRegistry
import de.jagenka.commands.discord.structure.ArgumentCombination
import de.jagenka.commands.discord.structure.ArgumentCombination.Companion.empty
import de.jagenka.commands.discord.structure.MessageCommand
import dev.kord.common.entity.Snowflake

object UnregisterCommand : MessageCommand
{
    override val ids: List<String>
        get() = listOf("unregister")
    override val helpText: String
        get() = "Unlink your current Discord User from the linked Minecraft Player."
    override val allowedArgumentCombinations: List<ArgumentCombination>
        get() = listOf(empty(helpText) { event ->
            event.message.author?.let {
                return@empty unregisterUser(it.id)
            }
            true
        })

    private suspend fun unregisterUser(userId: Snowflake): Boolean
    {
        val member = DiscordHandler.getMemberOrSendError(userId) ?: return false

        var response = ""

        UserRegistry.findUser(userId)?.let { oldUser ->
            MinecraftHandler.runWhitelistRemove(oldUser.minecraft.name)
            response += "`${oldUser.minecraft.name}` is no longer whitelisted.\n"
        }

        response +=
            if (UserRegistry.unregister(userId))
            {
                "${member.prettyName()} now unregistered."
            } else
            {
                "${member.prettyName()} was not registered."
            }

        DiscordHandler.sendMessage(response)

        UserRegistry.saveToFile()

        return true
    }
}