package de.jagenka

import de.jagenka.MinecraftHandler.logger
import net.minecraft.stat.ServerStatHandler
import net.minecraft.util.PathUtil
import net.minecraft.util.WorldSavePath
import java.io.File
import java.nio.file.Path
import java.util.*

object PlayerStatManager
{
    private val statisticsMap = mutableMapOf<UUID, ServerStatHandler>()

    fun getStatHandlerForPlayer(playerName: String): ServerStatHandler?
    {
        return getStatHandlerForPlayer(UserRegistry.getGameProfile(playerName)?.id ?: return null)
    }

    fun getStatHandlerForPlayer(uuid: UUID): ServerStatHandler?
    {
        // if player is online, get stathandler from playermanager
        MinecraftHandler.minecraftServer?.let { server ->
            server.playerManager.getPlayer(uuid)?.let { serverPlayerEntity ->
                val statHandler = server.playerManager.createStatHandler(serverPlayerEntity)
                this.statisticsMap[uuid] = statHandler
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

    // code largely copied from original minecraft source
    private fun loadStatHandlerFromFile(uuid: UUID, playerName: String = ""): ServerStatHandler?
    {
        MinecraftHandler.minecraftServer?.let { server ->
            val statsSavePath: File = server.getSavePath(WorldSavePath.STATS).toFile()
            val playerStatFile = File(statsSavePath, "${uuid}.json")
            if (playerName.isNotBlank())
            {
                val legacyPlayerStatFile = File(statsSavePath, "${playerName}.json")
                val legacyPath: Path = legacyPlayerStatFile.toPath()
                if (!playerStatFile.exists() && PathUtil.isNormal(legacyPath) && PathUtil.isAllowedName(legacyPath) && legacyPath.startsWith(statsSavePath.path) && legacyPlayerStatFile.isFile)
                {
                    legacyPlayerStatFile.renameTo(playerStatFile) //backwards compat to rename to UUID
                }
            }
            try
            {
                if (playerStatFile.exists())
                {
                    return ServerStatHandler(server, playerStatFile)
                }
            } catch (_: Exception)
            {
                logger.error("error parsing stat file, uuid might be invalid")
            }
        }

        return null
    }
}