package de.jagenka.commands

import com.google.gson.internal.Streams
import com.google.gson.stream.JsonReader
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import de.jagenka.Main
import de.jagenka.Users
import de.jagenka.Users.onlyMinecraftNames
import de.jagenka.Util.ticksToPrettyString
import de.jagenka.Util.unwrap
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.stat.Stats
import net.minecraft.text.Text
import net.minecraft.util.WorldSavePath
import java.io.StringReader
import java.nio.file.Files
import java.util.*
import kotlin.io.path.name
import kotlin.io.path.readText

object PlaytimeCommand : Command
{
    override fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
    {
        dispatcher.register(
            CommandManager.literal("playtime")
                .executes
                {
                    handle(it, "")
                    return@executes 0
                }
                .then(
                    CommandManager.argument("name", StringArgumentType.greedyString()).executes
                    {
                        handle(it, StringArgumentType.getString(it, "name"))
                        return@executes 0
                    })
        )
    }

    private fun handle(it: CommandContext<ServerCommandSource>, input: String)
    {
        getPlaytimeLeaderboardStrings(input).forEach { line ->
            it.source.sendFeedback(Text.literal(line), false)
        }
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

    private fun readPlaytimeFromStatsFiles(): Map<String, Int>
    {
        Main.minecraftServer?.let { server ->
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
        Main.minecraftServer?.let { server ->
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
}