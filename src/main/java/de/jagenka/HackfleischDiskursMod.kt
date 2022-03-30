package de.jagenka

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.MessageType
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.stat.Stats
import net.minecraft.text.LiteralText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Position
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.time.Duration
import java.util.*
import kotlin.math.min

//TODO playtime command
//TODO command interface

@Suppress("UNUSED")
object HackfleischDiskursMod : ModInitializer
{
    private const val MOD_ID = "hackfleisch-diskurs-mod"

    var uuid: UUID = UUID.randomUUID()

    private lateinit var minecraftServer: MinecraftServer

    override fun onInitialize() //TODO: schedule for later?
    {
        //register commands
        CommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            WhoisCommand.register(dispatcher)
            WhereIsCommand.register(dispatcher)
        }

        val path = FabricLoader.getInstance().configDir.resolve("hackfleisch-diskurs.yaml")
        val confLoader = YamlConfigurationLoader.builder().path(path).build()
        val root = confLoader.load()

        val token = root.node("bot-token").get(String::class.java)
        val guildId = root.node("bot-guild").get(Long::class.java)
        val channelId = root.node("bot-channel").get(Long::class.java)

        if (token == null)
        {
            root.node("bot-token").set("INSERT_TOKEN_HERE")
            confLoader.save(root)
            println("bot-token missing!")
        }
        if (guildId == null)
        {
            root.node("bot-guild").set("INSERT_GUILD_ID_HERE")
            confLoader.save(root)
            println("bot-guild missing!")
        }
        if (channelId == null)
        {
            root.node("bot-channel").set("INSERT_CHANNEL_ID_HERE")
            confLoader.save(root)
            println("bot-channel missing!")
        }
        if (token != null && guildId != null && channelId != null) DiscordBot.initialize(token, guildId, channelId)

        println("hackfleisch-diskurs-mod has been initialized.")
    }

    /**
     * @return Pair of playerName and deathCount
     */
    fun getDeathScore(playerName: String): Pair<String, Int>?
    {
        if (!checkMinecraftServer()) return null
        minecraftServer.scoreboard.getAllPlayerScores(minecraftServer.scoreboard.getObjective("deaths"))
            .forEach { if (it.playerName.equals(playerName, ignoreCase = true)) return Pair(it.playerName, it.score) }

        return null
    }

    /**
     * @return Pair of playerName and playtime in ticks
     */
    fun getPlaytime(playerName: String): Pair<String, Int>?
    {
        if (!checkMinecraftServer()) return null
        minecraftServer.playerManager.playerList.forEach { //TODO: this only works for online player -> browse MinecraftServer class. Maybe look for World save
            if (it.name.asString().equals(playerName, ignoreCase = true))
            {
                val playtime = it.statHandler.getStat(Stats.CUSTOM, Stats.PLAY_TIME)
                return Pair(it.name.asString(), playtime)
            }
        }

        return null
    }

    fun getScoreFromScoreboard() //TODO
    {
        minecraftServer.scoreboard.getAllPlayerScores(minecraftServer.scoreboard.getObjective("deaths")).forEach { println("${it.playerName}: ${it.score}") }
    }

    fun getOnlinePlayers(): List<String>
    {
        if (!checkMinecraftServer()) return emptyList()
        val list = ArrayList<String>()
        minecraftServer.playerManager.playerList.forEach { list.add(it.name.asString()) }
        return list
    }

    @JvmStatic
    fun broadcastMessage(message: String, formatting: Formatting = Formatting.WHITE, sender: UUID = uuid)
    {
        if (!checkMinecraftServer()) return
        val text = LiteralText(message).formatted(formatting)
        minecraftServer.playerManager.broadcast(text, MessageType.CHAT, sender)
    }

    fun doThing()
    {
        if (!checkMinecraftServer()) return
        //minecraftServer.commandManager.execute(minecraftServer.commandSource, "say helö") //cmd coming from MinecraftServer
        //minecraftServer.playerManager.playerList.get(0).dataTracker ??  Stats.PLAY_TIME
//        minecraftServer.playerManager.playerList.forEach {
//            val playtime = it.statHandler.getStat(Stats.CUSTOM, Stats.PLAY_TIME)
//            println("${it.name.asString()} has played for $playtime ticks")
//        }
    }

    fun runCommand(cmd: String)
    {
        if (!checkMinecraftServer()) return
        minecraftServer.commandManager.execute(minecraftServer.commandSource, cmd)
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
        if (!checkMinecraftServer()) return PerformanceMetrics(0.0, 0.0)
        val mspt = MathHelper.average(minecraftServer.lastTickLengths) * 1.0E-6
        val tps = min(1000.0 / mspt, 20.0)

        return PerformanceMetrics(mspt, tps)
    }

    fun getPlayerPosition(playerString: String): Position?
    {
        if (!checkMinecraftServer()) return null
        val player = minecraftServer.playerManager.getPlayer(playerString) ?: return null
        return player.pos
    }

    private fun checkMinecraftServer(): Boolean
    {
        return HackfleischDiskursMod::minecraftServer.isInitialized
    }

    fun sendMessageToPlayer(player: ServerPlayerEntity, text: String)
    {
        player.sendMessage(Text.of(text), MessageType.CHAT, uuid)
    }

    //to set MinecraftServer instance coming from Mixin
    @JvmStatic
    fun onServerLoaded(minecraftServer: MinecraftServer)
    {
        this.minecraftServer = minecraftServer
    }
}

data class PerformanceMetrics(val mspt: Double, val tps: Double)