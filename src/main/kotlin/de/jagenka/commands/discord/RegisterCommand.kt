package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.DiscordHandler
import de.jagenka.DiscordHandler.prettyName
import de.jagenka.Main
import de.jagenka.MinecraftHandler
import de.jagenka.UserRegistry
import de.jagenka.commands.DiskordelSlashCommand
import de.jagenka.commands.DiskordelTextCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.argument
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.RootInputChatBuilder
import dev.kord.rest.builder.interaction.string
import kotlinx.coroutines.launch

object RegisterCommand : DiskordelTextCommand, DiskordelSlashCommand
{
    override val name: String
        get() = "register"
    override val description: String
        get() = "Link your Discord Account to your Minecraft Name. This will also whitelist you."

    override suspend fun build(builder: RootInputChatBuilder)
    {
        with(builder)
        {
            string("minecraft_name", "Your in-game Minecraft name.") {
                required = true
            }
        }
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        with(event)
        {
            val minecraftName = interaction.command.strings["minecraft_name"]!!
            val discordId = interaction.user.id
            registerUser(discordId, minecraftName, interaction)
            // response should be handled by registerUser
        }
    }

    /**
     *
     */
    private suspend fun registerUser(userId: Snowflake, minecraftName: String, interaction: ChatInputCommandInteraction? = null)
    {
        val member = DiscordHandler.getMemberOrSendError(userId) ?: return

        val oldUser = UserRegistry.findUser(userId)

        UserRegistry.register(userId, minecraftName) { success ->
            var response = ""

            if (success)
            {
                oldUser?.let {
                    MinecraftHandler.runWhitelistRemove(oldUser.minecraft.username)
                    response += "- `${oldUser.minecraft.username}` is no longer whitelisted.\n"
                }

                val realName = UserRegistry.findUser(userId)?.minecraft?.username ?: minecraftName

                MinecraftHandler.runWhitelistAdd(realName)

                response += "- `$realName` now assigned to ${member.prettyName()}.\n" +
                        "- `$realName` is now whitelisted."
            } else
            {
                response += "Error registering."
            }

            Main.scope.launch {
                interaction?.respondEphemeral {
                    content = response
                }
            }

            UserRegistry.saveToFile()
        }
    }

    override val shortHelpText: String
        get() = "whitelist yourself"
    override val longHelpText: String
        get() = "link your Discord account to a Minecraft name. this will also whitelist you."

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        val commandNode = dispatcher.register(
            literal("register")
                .then(
                    argument<String>("minecraftName", StringArgumentType.word())
                        .executes {
                            Main.scope.launch {
                                DiscordHandler.sendMessage("use slash command!", silent = true)
                            }
                            0
                        }
                )
        )

        Registry.registerShortHelpText(shortHelpText, commandNode)
        Registry.registerLongHelpText(longHelpText, commandNode)
    }
}