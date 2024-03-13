package de.jagenka.commands.universal

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.DiscordHandler.asCodeBlock
import de.jagenka.UserRegistry
import de.jagenka.UserRegistry.getPrettyUsersList
import de.jagenka.commands.DiskordelSlashCommand
import de.jagenka.commands.DiskordelTextCommand
import de.jagenka.commands.MinecraftCommand
import de.jagenka.commands.discord.MessageCommandSource
import de.jagenka.commands.discord.Registry
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.RootInputChatBuilder
import dev.kord.rest.builder.interaction.string
import kotlinx.coroutines.runBlocking
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text


object WhoisCommand : DiskordelTextCommand, MinecraftCommand, DiskordelSlashCommand
{
    override val name: String
        get() = "who"
    override val description: String
        get() = "Identify users by a part of any of their names."

    override suspend fun build(builder: RootInputChatBuilder)
    {
        with(builder)
        {
            string("part_of_name", "Part of a player's name.")
            { required = true }
        }
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        with(event)
        {
            val partOfName = interaction.command.strings["part_of_name"]!!
            interaction.respondEphemeral {
                content = generateOutput(partOfName).asCodeBlock()
            }
        }
    }

    private fun generateOutput(partOfName: String = ""): String
    {
        val possibleUsers = UserRegistry.findRegistered(partOfName.trim())

        if (possibleUsers.isEmpty()) return "No-one found!"

        return runBlocking { "Could be:\n\n" + possibleUsers.getPrettyUsersList() }
    }

    override val shortHelpText: String
        get() = "identify players by name"
    override val longHelpText: String
        get() = "get all known names to a given part of name (Discord shown name, Discord username and Minecraft name)."

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        val commandNode = dispatcher.register(
            MessageCommandSource.literal("whois")
                .then(
                    MessageCommandSource.argument<String>("partOfName", StringArgumentType.word())
                        .executes
                        {
                            val output = generateOutput(StringArgumentType.getString(it, "partOfName"))
                            it.source.sendCodeBlock(output)
                            return@executes 0
                        })
        )

        Registry.registerShortHelpText(shortHelpText, commandNode)
        Registry.registerLongHelpText(longHelpText, commandNode)
    }

    override fun registerWithMinecraft(dispatcher: CommandDispatcher<ServerCommandSource>)
    {
        dispatcher.register(
            CommandManager.literal("whois")
                .then(
                    CommandManager.argument("partOfName", StringArgumentType.word()).executes
                    {
                        val output = generateOutput(StringArgumentType.getString(it, "partOfName"))
                        output.lines().forEach { line ->
                            if (line.isBlank()) return@forEach
                            it.source.sendFeedback({ Text.literal(line) }, false)
                        }
                        return@executes 0
                    })
        )
    }
}
