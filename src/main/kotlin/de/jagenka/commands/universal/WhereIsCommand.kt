package de.jagenka.commands.universal

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.MinecraftHandler
import de.jagenka.UserRegistry
import de.jagenka.commands.DiskordelSlashCommand
import de.jagenka.commands.MinecraftCommand
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.RootInputChatBuilder
import dev.kord.rest.builder.interaction.string
import net.minecraft.commands.Commands
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level

object WhereIsCommand : MinecraftCommand, DiskordelSlashCommand
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
