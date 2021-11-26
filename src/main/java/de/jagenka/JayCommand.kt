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
                HackfleischDiskursMod.broadcastMessage("Jay!", Formatting.WHITE, it.source.player.uuid)
                return@executes 0
            })
        }
    }
}