package de.jagenka.commands.universal

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.MinecraftHandler
import de.jagenka.UserRegistry
import de.jagenka.commands.DiskordelTextCommand
import de.jagenka.commands.MinecraftCommand
import de.jagenka.commands.discord.MessageCommandSource
import de.jagenka.commands.discord.MessageCommandSource.Companion.argument
import de.jagenka.commands.discord.Registry
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.world.World

object WhereIsCommand : DiskordelTextCommand, MinecraftCommand
{
    override val internalId: String
        get() = "whereis"

    private fun process(input: String): String
    {
        val possibleUsers = UserRegistry.findRegistered(input.trim())

        if (possibleUsers.isEmpty()) return "No-one found!"

        return possibleUsers.joinToString("\n") { user ->
            MinecraftHandler.minecraftServer?.let { server ->
                val player = server.playerManager.getPlayer(user.minecraft.uuid)
                    ?: return@let null

                val dimensionName = when (player.serverWorld.registryKey)
                {
                    World.OVERWORLD -> "Overworld"
                    World.NETHER -> "Nether"
                    World.END -> "End"
                    else -> return@let null
                }

                "${user.minecraft.name} is at (${player.x.toInt()}, ${player.y.toInt()}, ${player.z.toInt()}) in the $dimensionName."
            } ?: "${user.minecraft.name} is not online."
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
                .then(argument<String>("partOfName", StringArgumentType.greedyString())
                    .executes {
                        val output = process(StringArgumentType.getString(it, "partOfName"))
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
            CommandManager.literal("whereis")
                .then(CommandManager.argument("partOfName", StringArgumentType.greedyString())
                    .executes {
                        val output = process(StringArgumentType.getString(it, "partOfName"))
                        output.lines().forEach { line ->
                            if (line.isBlank()) return@forEach
                            it.source.sendFeedback({ Text.literal(line) }, false)
                        }
                        return@executes 0
                    })
        )
    }
}