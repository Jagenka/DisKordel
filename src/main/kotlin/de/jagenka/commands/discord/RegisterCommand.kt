package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.DiscordHandler.prettyName
import de.jagenka.MinecraftHandler
import de.jagenka.UserRegistry
import de.jagenka.commands.discord.structure.Argument.Companion.string
import de.jagenka.commands.discord.structure.ArgumentCombination
import de.jagenka.commands.discord.structure.ArgumentCombination.Companion.findInput
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
                return@ArgumentCombination registerUser(it.id, arguments.findInput("minecraftName"))
            }
            true
        })

    private suspend fun registerUser(userId: Snowflake, minecraftName: String): Boolean
    {
        val member = DiscordHandler.getMemberOrSendError(userId) ?: return false

        var response = ""

        val oldUser = UserRegistry.findUser(userId)

        UserRegistry.register(userId, minecraftName) { success ->
            if (success)
            {
                oldUser?.let {
                    MinecraftHandler.runWhitelistRemove(oldUser.minecraft.name)
                    response += "`${oldUser.minecraft.name}` is no longer whitelisted.\n"
                }

                val realName = UserRegistry.findUser(userId)?.minecraft?.name ?: minecraftName

                MinecraftHandler.runWhitelistAdd(realName)

                response += "`$realName` now assigned to ${member.prettyName()}.\n" +
                        "`$realName` is now whitelisted."
            } else
            {
                response += "Error registering."
            }
        }

        DiscordHandler.sendMessage(response)

        UserRegistry.saveToFile()

        return true
    }
}