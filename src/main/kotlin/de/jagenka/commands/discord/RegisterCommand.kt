package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.DiscordHandler.getPrettyMemberName
import de.jagenka.DiscordHandler.handleNotAMember
import de.jagenka.MinecraftHandler
import de.jagenka.Users
import de.jagenka.commands.discord.structure.Argument.Companion.string
import de.jagenka.commands.discord.structure.ArgumentCombination
import de.jagenka.commands.discord.structure.MessageCommand
import dev.kord.common.entity.Snowflake

object RegisterCommand : MessageCommand
{
    override val ids: List<String>
        get() = listOf("register")
    override val helpText: String
        get() = "Link your Discord User to your Minecraft Player."
    override val allowedArgumentCombinations: List<ArgumentCombination>
        get() = listOf(ArgumentCombination(string("minecraftName"), "Links your Minecraft name to your Discord account.") { event, arguments ->
            event.message.author?.let {
                return@ArgumentCombination registerUser(it.id, arguments["minecraftName"].toString())
            }
            true
        })

    private suspend fun registerUser(userId: Snowflake, minecraftName: String): Boolean
    {
        val member = DiscordHandler.guild.getMemberOrNull(userId)
        if (member == null)
        {
            handleNotAMember(userId)
            return false
        }

        if (Users.containsValue(minecraftName))
        {
            DiscordHandler.sendMessage("$minecraftName is already assigned to ${getPrettyMemberName(member)}")
            return false
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

        return true
    }
}