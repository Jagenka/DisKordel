package de.jagenka

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource

class JayCommand
{
    companion object
    {
        fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
        {
            dispatcher.register(CommandManager.literal("jay").executes {
                println("Source: ${it.source.name}")
                return@executes 0
            })
        }
    }
}