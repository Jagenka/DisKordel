package de.jagenka.stats

import de.jagenka.MinecraftHandler
import de.jagenka.MinecraftHandler.logger
import de.jagenka.UserRegistry
import net.minecraft.world.entity.player.Player
import net.minecraft.server.players.PlayerList
import net.minecraft.stats.ServerStatsCounter
import net.minecraft.world.level.storage.LevelResource
import net.minecraft.util.FileUtil
import java.io.File
import java.nio.file.Path
import java.util.*

object PlayerStatManager
{
    private val statisticsMap = mutableMapOf<UUID, ServerStatsCounter>()

    fun getStatHandlerForPlayer(playerName: String): ServerStatsCounter?
    {
        return getStatHandlerForPlayer(UserRegistry.getGameProfile(playerName)?.id ?: return null)
    }

    fun getStatHandlerForPlayer(uuid: UUID): ServerStatsCounter?
    {
        // if player is online, get stathandler from playermanager
        MinecraftHandler.minecraftServer?.let { server ->
            server.playerList.getPlayer(uuid)?.let { serverPlayerEntity ->
                val statHandler = server.playerList.getOrCreateStatHandler(serverPlayerEntity)
                statisticsMap[uuid] = statHandler
                return statHandler
            }
        }

        // if player is offline, check if already in storage
        if (!statisticsMap.containsKey(uuid))
        {
            // if not, load from file, save it to storage and return
            statisticsMap[uuid] = loadStatHandlerFromFile(uuid) ?: return null
        }

        return statisticsMap[uuid]
    }

    /**
     * this method should be called whenever there might be a change to a StatHandler, e.g. on creation of a new one (login)
     */
    fun updateStatHandler(uuid: UUID, serverStatHandler: ServerStatsCounter)
    {
        statisticsMap[uuid] = serverStatHandler
    }

    fun PlayerList.getOrCreateStatHandler(player: Player): ServerStatsCounter = this.getPlayerStats(player) // just an alias to better represent what this method does

    // code largely copied from original minecraft source
    private fun loadStatHandlerFromFile(uuid: UUID, playerName: String = ""): ServerStatsCounter?
    {
        MinecraftHandler.minecraftServer?.let { server ->
            val statsSavePath: File = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile()
            val playerStatFile = File(statsSavePath, "${uuid}.json")
            if (playerName.isNotBlank())
            {
                val legacyPlayerStatFile = File(statsSavePath, "${playerName}.json")
                val legacyPath: Path = legacyPlayerStatFile.toPath()
                if (!playerStatFile.exists() && FileUtil.isPathNormalized(legacyPath) && FileUtil.isPathPortable(legacyPath) && legacyPath.startsWith(statsSavePath.path) && legacyPlayerStatFile.isFile)
                {
                    legacyPlayerStatFile.renameTo(playerStatFile) //backwards compat to rename to UUID
                }
            }
            try
            {
                if (playerStatFile.exists())
                {
                    return ServerStatsCounter(server, playerStatFile.toPath())
                }
            } catch (_: Exception)
            {
                logger.error("error parsing stat file, uuid might be invalid")
            }
        }

        return null
    }
}