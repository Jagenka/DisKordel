package de.jagenka.commands.universal

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.MinecraftHandler
import de.jagenka.UserRegistry
import de.jagenka.commands.DiskordelSlashCommand
import de.jagenka.commands.DiskordelTextCommand
import de.jagenka.commands.MinecraftCommand
import de.jagenka.commands.discord.MessageCommandSource
import de.jagenka.commands.discord.MessageCommandSource.Companion.argument
import de.jagenka.commands.discord.Registry
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.RootInputChatBuilder
import dev.kord.rest.builder.interaction.string
import net.minecraft.commands.Commands
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level

object WhereIsCommand : DiskordelTextCommand, MinecraftCommand, DiskordelSlashCommand
{
    override val name: String
        get() = "where"
    override val description: String
        get() = "Get coordinates for an online player."

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
                content = process(partOfName)
            }
        }
    }

    private fun process(input: String): String
    {
        val possibleUsers = UserRegistry.findRegistered(input.trim())

        if (possibleUsers.isEmpty()) return "No-one found!"

        return possibleUsers.joinToString("\n") { user ->
            MinecraftHandler.minecraftServer?.let { server ->
                val player = server.playerList.getPlayer(user.minecraft.uuid)
                    ?: return@let null

                val dimensionName = when (player.level().dimension())
                {
                    Level.OVERWORLD -> "Overworld"
                    Level.NETHER -> "Nether"
                    Level.END -> "End"
                    else -> return@let null
                }

                "- ${user.minecraft.username} is at (${player.x.toInt()}, ${player.y.toInt()}, ${player.z.toInt()}) in the $dimensionName."
            } ?: "- ${user.minecraft.username} is not online."
        }
    }

    override val shortHelpText: String
        get() = "get player position"
    override val longHelpText: String
        get() = "get coordinates for players."

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        val commandNode = dispatcher.register(
            MessageCommandSource.literal("whereis")
                .then(
                    argument<String>("partOfName", StringArgumentType.greedyString())
                        .executes {
                            val output = process(StringArgumentType.getString(it, "partOfName"))
                            it.source.respond(output)
                            return@executes 0
                        })
        )

        Registry.registerShortHelpText(shortHelpText, commandNode)
        Registry.registerLongHelpText(longHelpText, commandNode)
    }

    override fun registerWithMinecraft(dispatcher: CommandDispatcher<CommandSourceStack>)
    {
        dispatcher.register(
            Commands.literal("whereis")
                .then(
                    Commands.argument("partOfName", StringArgumentType.greedyString())
                        .executes {
                            val output = process(StringArgumentType.getString(it, "partOfName"))
                            output.lines().forEach { line ->
                                if (line.isBlank()) return@forEach
                                it.source.sendSuccess({ Component.literal(line) }, false)
                            }
                            return@executes 0
                        })
        )
    }
}