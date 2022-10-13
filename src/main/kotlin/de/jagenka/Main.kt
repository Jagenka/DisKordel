package de.jagenka

import com.mojang.brigadier.CommandDispatcher
import de.jagenka.commands.DeathsCommand
import de.jagenka.commands.PlaytimeCommand
import de.jagenka.commands.WhereIsCommand
import de.jagenka.commands.WhoisCommand
import de.jagenka.config.Config
import de.jagenka.config.Config.configEntry
import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import java.util.*

//TODO command interface

@Suppress("UNUSED")
object Main : ModInitializer
{
    private const val MOD_ID = "hackfleisch-diskurs-mod"

    var uuid: UUID = UUID.randomUUID()

    val scope = MainScope()

    override fun onInitialize()
    {
        //register commands
        CommandRegistrationCallback.EVENT.register { dispatcher: CommandDispatcher<ServerCommandSource>, _: CommandRegistryAccess, _: CommandManager.RegistrationEnvironment ->
            WhoisCommand.register(dispatcher)
            WhereIsCommand.register(dispatcher)
            DeathsCommand.register(dispatcher)
            PlaytimeCommand.register(dispatcher)
        }

        Config.loadConfig()

        val token = configEntry.discordSettings.botToken
        val guildId = configEntry.discordSettings.guildId
        val channelId = configEntry.discordSettings.channelId

        // creating bot
        scope.launch { DiscordHandler.init(token, Snowflake(guildId), Snowflake(channelId)) }

        println("hackfleisch-diskurs-mod has been initialized.")
    }
}

