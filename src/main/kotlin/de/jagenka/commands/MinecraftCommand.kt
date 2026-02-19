package de.jagenka.commands

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack

interface MinecraftCommand : DiskordelCommand
{
    /**
     * this should register the command with Minecraft
     */
    fun registerWithMinecraft(dispatcher: CommandDispatcher<CommandSourceStack>)
}