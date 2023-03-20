package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.DiscordHandler.getPrettyMemberName
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
        val member = DiscordHandler.guild.getMemberOrNull(userId)
        if (member == null)
        {
            DiscordHandler.handleNotAMember(userId)
            return false
        }

        val minecraftName = UserRegistry.getValueForKey(member).orEmpty()
        UserRegistry.unregister(member)
        MinecraftHandler.runWhitelistRemove(minecraftName)
        DiscordHandler.sendMessage(
            "${getPrettyMemberName(member)} now unregistered.\n" +
                    if (minecraftName.isNotBlank()) "$minecraftName is no longer whitelisted" else ""
        )

        return true
    }
}