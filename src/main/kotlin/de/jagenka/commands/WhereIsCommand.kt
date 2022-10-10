package de.jagenka.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import de.jagenka.Main
import net.minecraft.command.CommandSource.suggestMatching
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource

object WhereIsCommand : Command
{
    override fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
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
        val position = Main.getPlayerPosition(name)
        val player = context.source.player ?: return
        if (position == null)
        {
            Main.sendMessageToPlayer(player, "$name is not a valid player name!")
        } else
        {
            Main.sendMessageToPlayer(player, "$name is at ${position.x.toInt()} ${position.y.toInt()} ${position.z.toInt()}")
        }
    }
}