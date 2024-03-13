package de.jagenka.commands

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.server.command.ServerCommandSource

interface MinecraftCommand : DiskordelCommand
{
    /**
     * this should register the command with Minecraft
     */
    fun registerWithMinecraft(dispatcher: CommandDispatcher<ServerCommandSource>)
}