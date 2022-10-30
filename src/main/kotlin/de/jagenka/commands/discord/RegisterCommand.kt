package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.DiscordHandler.getPrettyMemberName
import de.jagenka.DiscordHandler.handleNotAMember
import de.jagenka.Main
import de.jagenka.MinecraftHandler
import de.jagenka.Users
import dev.kord.common.entity.Snowflake
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.launch

object RegisterCommand : DiscordCommand
{
    override val discordName: String
        get() = "register"
    override val helpText: String
        get() = "`${DiscordCommandRegistry.commandPrefix}register minecraftName`: Link your current Discord User to your Minecraft Player."

    override fun execute(event: MessageCreateEvent, args: String)
    {
        event.message.author?.let {
            Main.scope.launch {
                registerUser(it.id, args.trim())
            }
        }
    }

    private suspend fun registerUser(userId: Snowflake, minecraftName: String)
    {
        val member = DiscordHandler.guild.getMemberOrNull(userId)
        if (member == null)
        {
            handleNotAMember(userId)
            return
        }

        if (Users.containsValue(minecraftName))
        {
            DiscordHandler.sendMessage("$minecraftName is already assigned to ${getPrettyMemberName(member)}")
            return
        }

        val oldName = Users.getValueForKey(member).orEmpty()
        Users.registerUser(member, minecraftName)
        MinecraftHandler.runWhitelistRemove(oldName)
        MinecraftHandler.runWhitelistAdd(minecraftName)
        DiscordHandler.sendMessage(
            "$minecraftName now assigned to ${getPrettyMemberName(member)}\n" +
                    "$minecraftName is now whitelisted" +
                    if (oldName.isNotEmpty()) "\n$oldName is no longer whitelisted" else ""
        )

        Users.saveToFile()
    }
}