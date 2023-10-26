package de.jagenka.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.commands.discord.MessageCommandSource
import de.jagenka.commands.discord.MessageCommandSource.Companion.argument
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

abstract class StringInStringOutCommand(val name: String) : MinecraftCommand, DiscordCommand
{
    /**
     * sends all lines of return value of process()
     */
    override fun registerWithMinecraft(dispatcher: CommandDispatcher<ServerCommandSource>)
    {
        dispatcher.register(
            CommandManager.literal(name)
                .executes {
                    val output = process().removePrefix("```").removeSuffix("```")
                    output.lines().toSet().forEach { line ->
                        if (line.isBlank()) return@forEach
                        it.source.sendFeedback({ Text.literal(line) }, false)
                    }
                    return@executes 0
                }
                .then(
                    CommandManager.argument("name", StringArgumentType.greedyString()).executes
                    {
                        val output = process(StringArgumentType.getString(it, "name")).removePrefix("```").removeSuffix("```")
                        output.lines().forEach { line ->
                            if (line.isBlank()) return@forEach
                            it.source.sendFeedback({ Text.literal(line) }, false)
                        }
                        return@executes 0
                    })
        )
    }

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        dispatcher.register(
            literal(name)
                .executes {
                    val output = process().removePrefix("```").removeSuffix("```")
                    it.source.sendFeedback(output)
                    return@executes 0
                }
                .then(
                    argument<String>("name", StringArgumentType.greedyString())
                        .executes
                        {
                            val output = process(StringArgumentType.getString(it, "name")).removePrefix("```").removeSuffix("```")
                            it.source.sendFeedback(output)
                            return@executes 0
                        })
        )
    }

    /**
     * @param input if this is empty, command should give back all information
     */
    abstract fun process(input: String = ""): String
}