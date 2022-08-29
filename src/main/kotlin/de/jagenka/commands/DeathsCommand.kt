package de.jagenka.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import de.jagenka.HackfleischDiskursMod
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object DeathsCommand : Command
{
    override fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
    {
        dispatcher.register(
            CommandManager.literal("deaths")
                .executes {
                    handle(it, "")
                    return@executes 0
                }
                .then(
                    CommandManager.argument("name", StringArgumentType.greedyString()).executes
                    {
                        handle(it, StringArgumentType.getString(it, "name"))
                        return@executes 0
                    })
        )
    }

    private fun handle(it: CommandContext<ServerCommandSource>, input: String)
    {
        HackfleischDiskursMod.getDeathLeaderboardStrings(input).forEach { line ->
            it.source.sendFeedback(Text.literal(line), false)
        }
    }
}