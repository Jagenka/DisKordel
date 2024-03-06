package de.jagenka.commands.universal

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.UserRegistry
import de.jagenka.commands.DiskordelTextCommand
import de.jagenka.commands.MinecraftCommand
import de.jagenka.commands.discord.MessageCommandSource
import de.jagenka.commands.discord.MessageCommandSource.Companion.argument
import de.jagenka.commands.discord.Registry
import de.jagenka.stats.StatUtil
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.stat.StatType
import net.minecraft.stat.Stats
import net.minecraft.text.Text

object PlaytimeCommand : DiskordelTextCommand, MinecraftCommand
{
    override val internalId: String
        get() = "playtime"

    @Suppress("UNCHECKED_CAST")
    private fun process(input: String = ""): String
    {
        return StatUtil.getStatReply(
            statType = Stats.CUSTOM as StatType<Any>,
            id = "play_time",
            queryType = StatUtil.StatQueryType.DEFAULT,
            nameFilter = if (input.isBlank()) emptyList() else UserRegistry.findMinecraftProfiles(input).map { it.name }
        )
    }

    override val shortHelpText: String
        get() = "get playtime for players"
    override val longHelpText: String
        get() = "list top 10 time spent on this server, filtered if argument exists. use `!stat custom play_time` if you want to see more."

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