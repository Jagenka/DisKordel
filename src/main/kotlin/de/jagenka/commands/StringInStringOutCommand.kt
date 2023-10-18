package de.jagenka.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.DiscordHandler
import de.jagenka.commands.discord.structure.Argument.Companion.string
import de.jagenka.commands.discord.structure.ArgumentCombination
import de.jagenka.commands.discord.structure.ArgumentCombination.Companion.empty
import de.jagenka.commands.discord.structure.ArgumentCombination.Companion.findInput
import de.jagenka.commands.discord.structure.MessageCommand
import de.jagenka.commands.discord.structure.Registry
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

interface StringInStringOutCommand : MinecraftCommand, MessageCommand
{
    /**
     * sends all lines of return value of process()
     */
    override fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
    {
        Registry.register(this)

        dispatcher.register(
            CommandManager.literal(minecraftName)
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

    val variableName: String

    override val allowedArgumentCombinations: List<ArgumentCombination>
        get() = listOf(
            empty(helpText) {
                DiscordHandler.sendCodeBlock(text = process(""))
                true
            },
            ArgumentCombination(string(variableName), helpText) { event, arguments ->
                DiscordHandler.sendCodeBlock(text = process(arguments.findInput(variableName)))
                true
            })

    /**
     * @param input if this is empty, command should give back all information
     */
    fun process(input: String = ""): String
}