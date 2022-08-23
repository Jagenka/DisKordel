package de.jagenka

import com.google.gson.internal.Streams
import com.google.gson.stream.JsonReader
import com.mojang.brigadier.CommandDispatcher
import de.jagenka.Users.onlyMinecraftNames
import de.jagenka.Util.unwrap
import de.jagenka.commands.DeathsCommand
import de.jagenka.commands.PlaytimeCommand
import de.jagenka.commands.WhereIsCommand
import de.jagenka.commands.WhoisCommand
import de.jagenka.config.Config
import de.jagenka.config.Config.configEntry
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.minecraft.command.CommandRegistryAccess
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.stat.Stats
import net.minecraft.text.Text
import net.minecraft.util.WorldSavePath
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Position
import java.io.StringReader
import java.nio.file.Files
import java.util.*
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.math.min

//TODO command interface

@Suppress("UNUSED")
object HackfleischDiskursMod : ModInitializer
{
    private const val MOD_ID = "hackfleisch-diskurs-mod"

    var uuid: UUID = UUID.randomUUID()

    var minecraftServer: MinecraftServer? = null
        private set

    override fun onInitialize() //TODO: schedule for later?
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

        DiscordBot.initialize(token, guildId, channelId)

        println("hackfleisch-diskurs-mod has been initialized.")
    }

    /**
     * @return List of Pair of real playerName and deathCount
     */
    fun getDeathScores(input: String): List<Pair<String, Int>> //TODO: get death count independent of scoreboard
    {
        minecraftServer?.let { server ->
            val result = mutableListOf<Pair<String, Int>>()

            val possiblePlayers = Users.find(input)

            server.scoreboard.getAllPlayerScores(server.scoreboard.getObjective("deaths"))
                .forEach {
                    possiblePlayers.forEach { player ->
                        if (it.playerName.equals(player.minecraftName, ignoreCase = true)) result.add(it.playerName to it.score)
                    }
                    if (it.playerName.equals(input, ignoreCase = true)) result.add(it.playerName to it.score)
                }

            return result.toList().sortedByDescending { it.second }
        }

        return emptyList()
    }

    /**
     * @return List of human-readable "leaderboard" entries of how often they died
     */
    fun getDeathLeaderboardStrings(input: String): List<String>
    {
        val result = mutableListOf<String>()
        getDeathScores(input).forEach { (playerName, deaths) ->
            result.add("$playerName has died $deaths time" + if (deaths != 1) "s" else "")
        }
        return result.toList()
    }

    /**
     * @return List of Pair of real playerName and playtime in ticks
     */
    fun getPlaytime(input: String): List<Pair<String, Int>>
    {
        val result = mutableListOf<Pair<String, Int>>()
        val possibleUsers = Users.find(input).onlyMinecraftNames().toMutableList()
        possibleUsers.add(input) // if someone is not registered
        getPlaytimeLeaderboard().forEach { pair -> possibleUsers.forEach { if (pair.first.lowercase().contains(it.lowercase())) result.add(pair) } }

        return result.distinctBy { it.first.lowercase() }
    }

    fun getPlaytimeLeaderboardStrings(input: String): List<String>
    {
        val result = mutableListOf<String>()
        getPlaytime(input).forEach { (playerName, ticks) ->
            result.add("$playerName has played for ${ticksToPrettyString(ticks)}")
        }
        return result.toList()
    }

    fun ticksToPrettyString(ticks: Int): String
    {
        val seconds = ticks / 20
        val minutes = seconds / 60
        val hours = minutes / 60

        val sb = StringBuilder()
        if (hours > 0) sb.append("${hours}h")
        if (hours > 0 || minutes > 0) sb.append(" ${minutes - hours * 60}min")
        if (hours > 0 || minutes > 0 || seconds > 0) sb.append(" ${seconds - minutes * 60}s")
        else sb.append("0h 0min 0s")

        return sb.toString().trim()
    }

    private fun readPlaytimeFromStatsFiles(): Map<String, Int>
    {
        minecraftServer?.let { server ->
            val statsPath = server.getSavePath(WorldSavePath.STATS)

            val statsOnDiskMap = HashMap<String, Int>() // key: minecraftName, value: playtime

            Files.list(statsPath).forEach { statFile ->
                val jsonReader = JsonReader(StringReader(statFile.readText()))
                val jsonElement = Streams.parse(jsonReader)
                val jsonObject = jsonElement.asJsonObject

                jsonObject.entrySet().find { it.key == "stats" }
                    ?.let {
                        it.value.asJsonObject.entrySet().find { it.key == "minecraft:custom" }
                            ?.let {
                                it.value.asJsonObject.entrySet().find { it.key == "minecraft:play_time" }
                                    ?.let { playtimeEntry ->
                                        val playerUUID = statFile.fileName.name.dropLast(5)
                                        statsOnDiskMap[server.userCache.getByUuid(UUID.fromString(playerUUID)).unwrap()?.name.toString()] =
                                                //TODO: seems to return null sometimes -> try to get name from someplace else than userCache
                                            playtimeEntry.value.asInt
                                    }
                            }

                    }
            }

            return statsOnDiskMap
        }

        return emptyMap()
    }

    fun getPlaytimeLeaderboard(): List<Pair<String, Int>>
    {
        minecraftServer?.let { server ->
            val leaderboardMap = HashMap<String, Int>()
            val statsFromFilesMap = readPlaytimeFromStatsFiles()

            statsFromFilesMap.forEach { (name, playtime) ->
                leaderboardMap[name] = playtime
            }

            server.playerManager.playerList.forEach { onlinePlayer ->
                val playtime = onlinePlayer.statHandler.getStat(Stats.CUSTOM, Stats.PLAY_TIME)
                leaderboardMap[onlinePlayer.name.string] = playtime
            }

            return leaderboardMap.toList().sortedByDescending { it.second }
        }

        return emptyList()
    }

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
        getPlaytimeLeaderboard().forEach { println(it) }

//        if (!checkMinecraftServer()) return
//
//        val hideo = minecraftServer.userCache.findByName("HideoTurismo").unwrap()?.id.toString()
//        val statsPath = minecraftServer.getSavePath(WorldSavePath.STATS)
//        Files.list(statsPath).forEach { statFile ->
//            if (statFile.toString().contains(hideo))
//            {
//                val jsonReader = JsonReader(StringReader(statFile.readText()))
//                val jsonElement = Streams.parse(jsonReader)
//                val jsonObject = jsonElement.asJsonObject
//
//                jsonObject.entrySet().find { it.key == "stats" }
//                    ?.let {
//                        it.value.asJsonObject.entrySet().find { it.key == "minecraft:custom" }
//                            ?.let {
//                                it.value.asJsonObject.entrySet().find { it.key == "minecraft:play_time" }
//                                    ?.let { println(it.value) }
//                            }
//                    }
//            }
//        }


        //minecraftServer.commandManager.execute(minecraftServer.commandSource, "say helö") //cmd coming from MinecraftServer
        //minecraftServer.playerManager.playerList.get(0).dataTracker ??  Stats.PLAY_TIME
//        minecraftServer.playerManager.playerList.forEach {
//            val playtime = it.statHandler.getStat(Stats.CUSTOM, Stats.PLAY_TIME)
//            println("${it.name.asString()} has played for $playtime ticks")
//        }
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