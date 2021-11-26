package de.jagenka

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.Formatting

class JayCommand
{
    companion object
    {
        fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
        {
            dispatcher.register(CommandManager.literal("jay").executes {
                HackfleischDiskursMod.broadcastMessage(it.source, Formatting.WHITE, "Jay!", it.source.player.uuid)
                return@executes 0
            })
        }
    }
}