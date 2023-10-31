package de.jagenka.commands.universal

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.UserRegistry
import de.jagenka.commands.DiscordCommand
import de.jagenka.commands.MinecraftCommand
import de.jagenka.commands.discord.MessageCommandSource
import de.jagenka.commands.discord.MessageCommandSource.Companion.argument
import de.jagenka.commands.discord.Registry
import de.jagenka.commands.discord.StatsCommand
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.stat.StatType
import net.minecraft.stat.Stats
import net.minecraft.text.Text

object PlaytimeCommand : DiscordCommand, MinecraftCommand
{
    @Suppress("UNCHECKED_CAST")
    private fun process(input: String = ""): String
    {
        return if (input.isBlank())
        {
            StatsCommand.getReplyForAll(Stats.CUSTOM as StatType<Any>, "play_time")
        } else
        {
            StatsCommand.getReplyForSome(UserRegistry.findMinecraftProfiles(input), Stats.CUSTOM as StatType<Any>, "play_time")
        }
    }

    override val shortHelpText: String
        get() = "get playtime for players"
    override val longHelpText: String
        get() = "list all players' time spent on this server, filtered if argument exists."

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        val commandNode = dispatcher.register(
            MessageCommandSource.literal("playtime")
                .executes {
                    val output = process()
                    it.source.sendCodeBlock(output)
                    0
                }
                .then(argument<String>("partOfName", StringArgumentType.greedyString())
                    .executes {
                        val output = process(StringArgumentType.getString(it, "partOfName"))
                        it.source.sendCodeBlock(output)
                        0
                    })
        )

        Registry.registerShortHelpText(shortHelpText, commandNode)
        Registry.registerLongHelpText(longHelpText, commandNode)
    }

    override fun registerWithMinecraft(dispatcher: CommandDispatcher<ServerCommandSource>)
    {
        dispatcher.register(
            CommandManager.literal("playtime")
                .executes {
                    val output = process()
                    output.lines().forEach { line ->
                        if (line.isBlank()) return@forEach
                        it.source.sendFeedback({ Text.literal(line) }, false)
                    }
                    0
                }
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