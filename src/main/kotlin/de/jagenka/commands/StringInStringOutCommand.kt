package de.jagenka.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

interface StringInStringOutCommand : Command
{
    val literal: String

    override fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
    {
        dispatcher.register(
            CommandManager.literal(literal)
                .executes {
                    val output = execute("")
                    output.lines().forEach { line ->
                        if (line.isBlank()) return@forEach
                        it.source.sendFeedback(Text.literal(line), false)
                    }
                    return@executes 0
                }
                .then(
                    CommandManager.argument("name", StringArgumentType.greedyString()).executes
                    {
                        val output = execute(StringArgumentType.getString(it, "name"))
                        output.lines().forEach { line ->
                            if (line.isBlank()) return@forEach
                            it.source.sendFeedback(Text.literal(line), false)
                        }
                        return@executes 0
                    })
        )
    }

    /**
     * @param input: if this is empty, command is without argument
     */
    fun execute(input: String): String
}