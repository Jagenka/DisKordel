package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.DiscordHandler.getPrettyMemberName
import de.jagenka.Main
import de.jagenka.MinecraftHandler
import de.jagenka.Users
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.launch

object UnregisterCommand : DiscordCommand
{
    override val discordName: String
        get() = "unregister"

    override fun execute(event: MessageCreateEvent, args: String)
    {
        event.message.author?.let {
            Main.scope.launch {
                unregisterUser(it.id)
            }
        }
    }

    private suspend fun unregisterUser(userId: Snowflake)
    {
        val member = DiscordHandler.guild.getMemberOrNull(userId)
        if (member == null)
        {
            DiscordHandler.handleNotAMember(userId)
            return
        }

        val minecraftName = Users.getValueForKey(member).orEmpty()
        Users.unregisterUser(member)
        MinecraftHandler.runWhitelistRemove(minecraftName)
        DiscordHandler.sendMessage(
            "${getPrettyMemberName(member)} now unregistered.\n" +
                    if (minecraftName.isNotBlank()) "$minecraftName is no longer whitelisted" else ""
        )
    }
}