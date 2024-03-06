package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import de.jagenka.DiscordHandler
import de.jagenka.DiscordHandler.prettyName
import de.jagenka.Main
import de.jagenka.MinecraftHandler
import de.jagenka.UserRegistry
import de.jagenka.commands.DiskordelTextCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal
import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.launch

object UnregisterCommand : DiskordelTextCommand
{
    override val internalId: String
        get() = "unregister"

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

        DiscordHandler.sendMessage(response, silent = true)

        UserRegistry.saveToFile()

        return true
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
                            unregisterUser(user.id)
                        }
                    }
                    0
                }
        )

        Registry.registerShortHelpText(shortHelpText, commandNode)
        Registry.registerLongHelpText(longHelpText, commandNode)
    }
}