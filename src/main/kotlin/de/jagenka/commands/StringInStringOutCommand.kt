package de.jagenka.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

interface StringInStringOutCommand : Command
{
    val literal: String

    /**
     * sends all lines of return value of execute()
     */
    override fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
    {
        dispatcher.register(
            CommandManager.literal(literal)
                .executes {
                    val output = execute(it)
                    output.lines().forEach { line ->
                        if (line.isBlank()) return@forEach
                        it.source.sendFeedback(Text.literal(line), false)
                    }
                    return@executes 0
                }
                .then(
                    CommandManager.argument("name", StringArgumentType.greedyString()).executes
                    {
                        val output = execute(it, StringArgumentType.getString(it, "name"))
                        output.lines().forEach { line ->
                            if (line.isBlank()) return@forEach
                            it.source.sendFeedback(Text.literal(line), false)
                        }
                        return@executes 0
                    })
        )
    }

    /**
     * @return what is to be sent as feedback to sender
     */
    fun execute(ctx: CommandContext<ServerCommandSource>): String
    {
        return execute(ctx, "")
    }

    fun execute(ctx: CommandContext<ServerCommandSource>, input: String): String
}