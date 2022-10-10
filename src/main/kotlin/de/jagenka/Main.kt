package de.jagenka

import com.mojang.brigadier.CommandDispatcher
import de.jagenka.commands.DeathsCommand
import de.jagenka.commands.PlaytimeCommand
import de.jagenka.commands.WhereIsCommand
import de.jagenka.commands.WhoisCommand
import de.jagenka.config.Config
import de.jagenka.config.Config.configEntry
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Position
import java.util.*
import kotlin.math.min

//TODO command interface

@Suppress("UNUSED")
object Main : ModInitializer
{
    private const val MOD_ID = "hackfleisch-diskurs-mod"

    var uuid: UUID = UUID.randomUUID()

    var minecraftServer: MinecraftServer? = null
        private set

    var kord: Kord? = null
        private set

    private var link: HackfleischDiskursLink? = null

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
        scope.launch {
            kord = Kord(token)

            kord?.login {
                @OptIn(PrivilegedIntent::class)
                intents += Intent.MessageContent
            } ?: error("error logging in")

            kord?.on<MessageCreateEvent> {
                // return if author is a bot or undefined
                if (message.author?.isBot != false) return@on
                if (message.channelId != Snowflake(channelId)) return@on
                link?.handleDiscordMessage(this)
            } ?: error("error initializing message handling")

            link = HackfleischDiskursLink(Snowflake(guildId), Snowflake(channelId))
        }

        println("hackfleisch-diskurs-mod has been initialized.")
    }

    @JvmStatic
    fun handleMinecraftChatMessage(message: Text, sender: ServerPlayerEntity?)
    {
        scope.launch { link?.handleMinecraftChatMessage(message, sender) }
    }

    @JvmStatic
    fun handleMinecraftSystemMessage(message: Text)
    {
        scope.launch { link?.handleMinecraftSystemMessage(message) }
    }

    //TODO move everything after this line to own object

    fun getScoreFromScoreboard() //TODO
    {
        minecraftServer?.let { server ->
            server.scoreboard.getAllPlayerScores(server.scoreboard.getObjective("deaths")).forEach { println("${it.playerName}: ${it.score}") }
        }
    }

    fun getOnlinePlayers(): List<String>
    {
        minecraftServer?.let { server ->
            val list = ArrayList<String>()
            server.playerManager.playerList.forEach { list.add(it.name.string) }
            return list
        }

        return emptyList()
    }

    fun doThing()
    {

    }

    fun runCommand(cmd: String)
    {
        minecraftServer?.commandManager?.executeWithPrefix(minecraftServer?.commandSource, cmd)
    }

    fun runWhitelistAdd(player: String)
    {
        if (player.isEmpty()) return
        runCommand("whitelist add $player")
    }

    fun runWhitelistRemove(player: String)
    {
        if (player.isEmpty()) return
        runCommand("whitelist remove $player")
    }

    fun getPerformanceMetrics(): PerformanceMetrics
    {
        minecraftServer?.let { server ->
            val mspt = MathHelper.average(server.lastTickLengths) * 1.0E-6
            val tps = min(1000.0 / mspt, 20.0)

            return PerformanceMetrics(mspt, tps)
        }

        return PerformanceMetrics(0.0, 0.0)
    }

    fun getPlayerPosition(playerString: String): Position?
    {
        minecraftServer?.let { server ->
            val player = server.playerManager.getPlayer(playerString) ?: return null
            return player.pos
        }
        return null
    }

    fun sendMessageToPlayer(player: ServerPlayerEntity, text: String)
    {
        player.sendMessage(Text.of(text))
    }

    //to set MinecraftServer instance coming from Mixin
    @JvmStatic
    fun onServerLoaded(minecraftServer: MinecraftServer)
    {
        this.minecraftServer = minecraftServer

        minecraftServer.playerManager.isWhitelistEnabled = true
    }
}

data class PerformanceMetrics(val mspt: Double, val tps: Double)