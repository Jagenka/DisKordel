package de.jagenka.commands

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.server.command.ServerCommandSource

interface Command // TODO: Discord part
{
    /**
     * this should register the command with Minecraft
     */
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
}