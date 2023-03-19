package de.jagenka

import com.mojang.authlib.GameProfile
import de.jagenka.MinecraftHandler.logger
import de.jagenka.Util.unwrap
import de.jagenka.config.Config
import net.minecraft.server.WhitelistEntry
import net.minecraft.stat.ServerStatHandler
import net.minecraft.util.PathUtil
import net.minecraft.util.WorldSavePath
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.function.Function
import java.util.function.IntFunction
import java.util.function.Predicate

object PlayerStatManager
{
    private val statisticsMap = mutableMapOf<UUID, ServerStatHandler>()

    fun getPlayerNameFromUUID(uuid: UUID): String?
    {
        MinecraftHandler.minecraftServer?.playerManager?.whitelist?.values()!!.stream().map<GameProfile> { obj: WhitelistEntry -> obj.getKey() }.filter(Predicate { obj: GameProfile? ->
            Objects.nonNull(
                obj
            )
        }).map<String>(
            Function { obj: GameProfile -> obj.name }).toArray<String>(IntFunction<Array<String>> { _Dummy_.__Array__() });

        val playerName = Config.configEntry.users.find { it.uuid == uuid.toString() }?.minecraftName
            ?: MinecraftHandler.minecraftServer?.playerManager?.getPlayer(uuid)?.name?.string
            ?: MinecraftHandler.minecraftServer?.playerManager?.whitelist?.values()
            ?: MinecraftHandler.minecraftServer?.userCache?.getByUuid(uuid)?.unwrap()?.name

        // TODO: save to my config

        return playerName
    }

    fun getUUIDFromPlayerName(playerName: String): UUID?
    {
        val uuid = try
        {
            UUID.fromString(Config.configEntry.users.find { it.minecraftName.equals(playerName, ignoreCase = true) }?.uuid ?: "")
        } catch (_: IllegalArgumentException)
        {
            MinecraftHandler.minecraftServer?.playerManager?.getPlayer(playerName)?.uuid
                ?: MinecraftHandler.minecraftServer?.userCache?.findByName(playerName)?.unwrap()?.id
        }

        uuid?.let { Config.storeUUIDForPlayerName(playerName, uuid) }

        return uuid
    }

    fun getStatHandlerForPlayer(playerName: String): ServerStatHandler?
    {
        val uuid = getUUIDFromPlayerName(playerName)

        uuid?.let { return getStatHandlerForPlayer(it) }

        return null
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
            val statHandlerFromFile = loadStatHandlerFromFile(uuid)
            statisticsMap[uuid] = statHandlerFromFile ?: return null
        }

        return statisticsMap[uuid]
    }

    private fun loadStatHandlerFromFile(uuid: UUID, playerName: String = ""): ServerStatHandler?
    {
        MinecraftHandler.minecraftServer?.let { server ->
            val statsSavePath: File = server.getSavePath(WorldSavePath.STATS).toFile()
            val playerStatFile = File(statsSavePath, "${uuid}.json")
            if (playerName.isNotBlank())
            {
                val legacyPlayerStatFile = File(statsSavePath, "${playerName}.json")
                val path: Path = legacyPlayerStatFile.toPath()
                if (!playerStatFile.exists() && PathUtil.isNormal(path) && PathUtil.isAllowedName(path) && path.startsWith(statsSavePath.path) && legacyPlayerStatFile.isFile)
                {
                    legacyPlayerStatFile.renameTo(playerStatFile) //backwards compat to rename to UUID
                }
            }
            try
            {
                return ServerStatHandler(server, playerStatFile)
            } catch (_: Exception)
            {
                logger.error("error parsing stat file, uuid might be invalid")
            }
        }

        return null
    }
}