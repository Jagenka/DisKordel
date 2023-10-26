package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.DiscordHandler
import de.jagenka.DiscordHandler.prettyName
import de.jagenka.Main
import de.jagenka.MinecraftHandler
import de.jagenka.UserRegistry
import de.jagenka.commands.DiscordCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.argument
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal
import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.launch

object RegisterCommand : DiscordCommand
{
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
                    response += "- `${oldUser.minecraft.name}` is no longer whitelisted.\n"
                }

                val realName = UserRegistry.findUser(userId)?.minecraft?.name ?: minecraftName

                MinecraftHandler.runWhitelistAdd(realName)

                response += "- `$realName` now assigned to ${member.prettyName()}.\n" +
                        "- `$realName` is now whitelisted."
            } else
            {
                response += "Error registering."
            }
        }

        DiscordHandler.sendMessage(response, silent = true)

        UserRegistry.saveToFile()

        return true
    }

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        dispatcher.register(
            literal("register")
                .then(
                    argument<String>("minecraftName", StringArgumentType.word())
                        .executes {
                            Main.scope.launch {
                                it.source.author?.let { author ->
                                    registerUser(author.id, it.getArgument("minecraftName", String::class.java))
                                } ?: MinecraftHandler.logger.error("non-existent author tried to register")
                            }
                            0
                        }
                )
        )
    }
}