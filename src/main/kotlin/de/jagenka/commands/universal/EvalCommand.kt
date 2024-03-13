package de.jagenka.commands.universal

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.commands.DiskordelSlashCommand
import de.jagenka.commands.MinecraftCommand
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.RootInputChatBuilder
import dev.kord.rest.builder.interaction.number
import dev.kord.rest.builder.interaction.string
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import redempt.crunch.Crunch
import redempt.crunch.exceptions.ExpressionCompilationException
import redempt.crunch.exceptions.ExpressionEvaluationException
import redempt.crunch.functional.ExpressionEnv

object EvalCommand : DiskordelSlashCommand, MinecraftCommand
{
    private val env = ExpressionEnv()

    override val name: String
        get() = "evaluate"
    override val description: String
        get() = "Evaluate mathematical expressions."

    init
    {
        env.setVariableNames("x", "y")
        // add custom useful functions here
        env.addFunction("stack", 1) { (d) -> d / 64 } // convert item count to stacks
        env.addFunction("stacks", 1) { (d) -> d / 64 }
        env.addFunction("chest", 1) { (d) -> d / 64 / 27 } // convert item count to chests
        env.addFunction("chests", 1) { (d) -> d / 64 / 27 }
    }

    override suspend fun build(builder: RootInputChatBuilder)
    {
        with(builder)
        {
            string("expression", "Mathematical expression to evaluate.")
            { required = true }
            number("x", "Value of variable x in expression.")
            { required = false }
            number("y", "Value of variable y in expression.")
            { required = false }
        }
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        with(event)
        {
            val expression = interaction.command.strings["expression"]!!
            val x = interaction.command.numbers["x"]
            val y = interaction.command.numbers["y"]

            interaction.respondEphemeral {
                content = eval(expression, x, y)
            }
        }
        return
    }

    /**
     * variables are not supported
     */
    override fun registerWithMinecraft(dispatcher: CommandDispatcher<ServerCommandSource>)
    {
        dispatcher.register(
            CommandManager.literal("evaluate")
                .then(
                    CommandManager.argument("expression", StringArgumentType.greedyString()).executes
                    {
                        val output = eval(StringArgumentType.getString(it, "expression"))
                        output.lines().forEach { line ->
                            if (line.isBlank()) return@forEach
                            it.source.sendFeedback({ Text.literal(line) }, false)
                        }
                        return@executes 0
                    })
        )
    }

    /**
     * @return response - may be error message
     */
    private fun eval(expression: String, x: Double? = null, y: Double? = null): String
    {
        try
        {
            val exp = Crunch.compileExpression(expression, env)
            return exp.evaluate(x ?: 0.0, y ?: 0.0).toString()
        } catch (e: ExpressionCompilationException)
        {
            return e.message ?: "Error compiling expression."
        } catch (e: ExpressionEvaluationException)
        {
            return e.message ?: "Error evaluating expression."
        }
    }
}