package de.jagenka

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import de.jagenka.Util.trim
import net.minecraft.command.CommandSource.suggestMatching
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import kotlin.math.roundToInt

object WhereIsCommand
{
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
    {
        dispatcher.register(
            CommandManager.literal("whereis").then(
                CommandManager.argument("player", StringArgumentType.word())
                    .suggests { commandContext, suggestionsBuilder ->
                        suggestMatching(commandContext.source.playerNames, suggestionsBuilder)
                    }
                    .executes {
                        handleWhereIsCommand(it, StringArgumentType.getString(it, "player"))
                        return@executes 0
                    })
        )
    }

    private fun handleWhereIsCommand(context: CommandContext<ServerCommandSource>, name: String)
    {
        val position = HackfleischDiskursMod.getPlayerPosition(name)
        if (position == null)
        {
            HackfleischDiskursMod.sendMessageToPlayer(context.source.player, "$name is not a valid player name!")
        } else
        {
            HackfleischDiskursMod.sendMessageToPlayer(context.source.player, "$name is at ${position.x.toInt()} ${position.y.toInt()} ${position.z.toInt()}")
        }
    }
}