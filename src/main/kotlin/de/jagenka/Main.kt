package de.jagenka

import com.mojang.brigadier.CommandDispatcher
import de.jagenka.commands.universal.DeathsCommand
import de.jagenka.commands.universal.PlaytimeCommand
import de.jagenka.commands.universal.WhereIsCommand
import de.jagenka.commands.universal.WhoisCommand
import de.jagenka.config.Config
import de.jagenka.config.Config.configEntry
import de.jagenka.config.StatManager
import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource

//TODO command interface

@Suppress("UNUSED")
object Main : ModInitializer
{
    val scope: CoroutineScope = CoroutineScope(SupervisorJob())

    private val minecraftCommands = listOf(
        WhoisCommand,
        WhereIsCommand,
        DeathsCommand,
        PlaytimeCommand
    )

    override fun onInitialize()
    {
        //register commands
        CommandRegistrationCallback.EVENT.register { dispatcher: CommandDispatcher<ServerCommandSource>, _: CommandRegistryAccess, _: CommandManager.RegistrationEnvironment ->
            minecraftCommands.forEach { it.register(dispatcher) }
        }

        Config.loadConfig()
        StatManager.loadStats()

        val token = configEntry.discordSettings.botToken
        val guildId = configEntry.discordSettings.guildId
        val channelId = configEntry.discordSettings.channelId

        // creating bot
        scope.launch {
            println("launching bot...")
            DiscordHandler.init(token, Snowflake(guildId), Snowflake(channelId))
        }

        println("hackfleisch-diskurs-mod has been initialized.")
    }
}

