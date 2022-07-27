package de.jagenka.commands

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.server.command.ServerCommandSource

interface Command
{
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
}