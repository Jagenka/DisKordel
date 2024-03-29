package de.jagenka

import com.mojang.brigadier.CommandDispatcher
import de.jagenka.MinecraftHandler.logger
import de.jagenka.MinecraftHandler.registerMixins
import de.jagenka.commands.MinecraftCommand
import de.jagenka.commands.discord.Registry
import de.jagenka.config.Config
import de.jagenka.config.Config.configEntry
import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.*
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import kotlin.system.exitProcess

object Main : ModInitializer
{
    val scope: CoroutineScope = CoroutineScope(SupervisorJob())

    private var stoppingTask: Job? = null

    override fun onInitialize()
    {
        //register onServerLoaded
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            MinecraftHandler.onServerLoaded(server)
            scope.launch {
                DiscordHandler.sendWebhookMessage(
                    username = configEntry.discordSettings.serverName,
                    text = "> *Server started!*",
                    escapeMarkdown = false
                )
            }
        }

        //register onServerStopping
        ServerLifecycleEvents.SERVER_STOPPING.register { server ->
            stoppingTask = scope.launch {
                DiscordHandler.sendWebhookMessage(
                    username = configEntry.discordSettings.serverName,
                    text = "> *Server stopping...*",
                    escapeMarkdown = false
                )
                stoppingTask?.cancel("done")
            }
        }

        //register onServerStopped
        ServerLifecycleEvents.SERVER_STOPPED.register { server ->
            scope.launch {
                stoppingTask?.invokeOnCompletion {
                    logger.info("killing process now...")
                    exitProcess(0)
                } ?: exitProcess(0)
            }
        }

        registerMixins()

        //register commands
        CommandRegistrationCallback.EVENT.register { dispatcher: CommandDispatcher<ServerCommandSource>, _: CommandRegistryAccess, _: CommandManager.RegistrationEnvironment ->
            Registry.commands.filterIsInstance<MinecraftCommand>().forEach { it.registerWithMinecraft(dispatcher) }
        }

        Config.loadConfig()

        val token = configEntry.discordSettings.botToken
        val guildId = configEntry.discordSettings.guildId
        val channelId = configEntry.discordSettings.channelId

        // creating bot
        scope.launch {
            logger.info("launching bot...")
            DiscordHandler.init(token, Snowflake(guildId), Snowflake(channelId))
        }

        logger.info("DisKordel has been initialized.") // TODO: move, this appears before everything is done
    }
}

