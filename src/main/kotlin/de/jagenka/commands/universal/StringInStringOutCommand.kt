package de.jagenka.commands.universal

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.DiscordHandler
import de.jagenka.commands.discord.DiscordCommand
import de.jagenka.commands.discord.DiscordCommandRegistry
import de.jagenka.commands.minecraft.MinecraftCommand
import dev.kord.core.event.message.MessageCreateEvent
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

interface StringInStringOutCommand : MinecraftCommand, DiscordCommand
{
    /**
     * sends all lines of return value of process()
     */
    override fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
    {
        DiscordCommandRegistry.register(this)

        dispatcher.register(
            CommandManager.literal(minecraftName)
                .executes {
                    val output = process(it.source.name)
                    output.lines().toSet().forEach { line ->
                        if (line.isBlank()) return@forEach
                        it.source.sendFeedback(Text.literal(line), false)
                    }
                    return@executes 0
                }
                .then(
                    CommandManager.argument("name", StringArgumentType.greedyString()).executes
                    {
                        val output = process(StringArgumentType.getString(it, "name"))
                        output.lines().forEach { line ->
                            if (line.isBlank()) return@forEach
                            it.source.sendFeedback(Text.literal(line), false)
                        }
                        return@executes 0
                    })
        )
    }

    override fun execute(event: MessageCreateEvent, args: String)
    {
        val input = args.trim()
        DiscordHandler.sendMessage(process(input))
    }

    /**
     * @param input if this is empty, command should give back all information TODO test this
     */
    fun process(input: String): String
}