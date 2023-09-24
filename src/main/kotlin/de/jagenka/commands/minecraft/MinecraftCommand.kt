package de.jagenka.commands.minecraft

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.server.command.ServerCommandSource

interface MinecraftCommand
{
    val minecraftName: String

    /**
     * this should register the command with Minecraft
     */
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
}