package de.jagenka

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.minecraft.server.command.CommandManager

@Suppress("UNUSED")
object HackfleischDiskursMod : ModInitializer
{
    private const val MOD_ID = "hackfleisch-diskurs-mod"
    override fun onInitialize()
    {
        println("hackfleisch-diskurs-mod has been initialized.")

        CommandRegistrationCallback.EVENT.register { dispatcher, dedicated ->
            val jayCommand = CommandManager.literal("jay").executes {
                println("jay!")
                return@executes 0
            }

            dispatcher.register(jayCommand)
        }
    }
}