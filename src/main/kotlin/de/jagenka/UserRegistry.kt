package de.jagenka

import com.mojang.authlib.Agent
import com.mojang.authlib.GameProfile
import com.mojang.authlib.ProfileLookupCallback
import de.jagenka.DiscordHandler.prettyName
import de.jagenka.MinecraftHandler.logger
import de.jagenka.MinecraftHandler.minecraftServer
import de.jagenka.Util.unwrap
import de.jagenka.config.Config
import de.jagenka.config.UserEntry
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Member
import kotlinx.coroutines.launch
import net.minecraft.util.WorldSavePath
import java.nio.file.Files
import java.util.*

object UserRegistry
{
    private val registeredUsers = mutableSetOf<User>()

    private val discordMembers = mutableMapOf<DiscordUser, Member>() //TODO: load Members every so often (?)
    private var userCache = mutableSetOf<MinecraftUser>()
    private val minecraftProfiles = mutableSetOf<GameProfile>()

    // region getter
    fun findUser(minecraftName: String): User?
    {
        return registeredUsers.find { it.minecraft.name.equals(minecraftName, ignoreCase = true) }
    }

    fun findUser(uuid: UUID): User?
    {
        return registeredUsers.find { it.minecraft.uuid == uuid }
    }

    fun findUser(snowflake: Snowflake): User?
    {
        return registeredUsers.find { it.discord.id == snowflake }
    }

    fun getMinecraftUser(name: String): MinecraftUser?
    {
        return userCache.find { it.name == name }
    }

    fun getMinecraftUser(uuid: UUID): MinecraftUser?
    {
        return userCache.find { it.uuid == uuid }
    }

    fun getGameProfile(name: String): GameProfile?
    {
        return minecraftProfiles.find { it.name.equals(name, ignoreCase = true) }
    }

    fun getGameProfile(uuid: UUID): GameProfile?
    {
        return minecraftProfiles.find { it.id == uuid }
    }

    fun getDiscordUser(id: Snowflake): DiscordUser?
    {
        return findUser(id)?.discord
    }

    fun getDiscordMember(snowflake: Snowflake): Member?
    {
        return discordMembers[DiscordUser(snowflake)]
    }

    fun getDiscordMember(inputName: String): Member?
    {
        return discordMembers.values.find { it.username == inputName || it.effectiveName == inputName }
    }

    fun getAllRegisteredUsers() = registeredUsers.toList()

    fun getAllUsersAsOutput(): String
    {
        return getAllRegisteredUsers().joinToString(prefix = "Currently registered Users:\n", separator = "\n") { it.prettyString() }
    }

    fun getMinecraftProfiles() = minecraftProfiles.toSet()

    fun findRegistered(name: String): List<User>
    {
        return registeredUsers.filter {
            discordMembers[it.discord]?.username?.contains(name, ignoreCase = true) ?: false
                    || discordMembers[it.discord]?.effectiveName?.contains(name, ignoreCase = true) ?: false
                    || it.minecraft.name.contains(name, ignoreCase = true)
        }
    }

    fun findMinecraftProfiles(name: String): List<GameProfile>
    {
        return minecraftProfiles.filter {
            it.name.contains(name, ignoreCase = true)
        }
    }
    // endregion

    // region registration
    suspend fun register(snowflake: Snowflake, minecraftName: String): Boolean
    {
        if (!findMinecraftProfileOrError(minecraftName)) return false
        if (!findDiscordMemberOrError(snowflake)) return false

        val gameProfile = getGameProfile(minecraftName) ?: return false
        registeredUsers.put(User(DiscordUser(snowflake), MinecraftUser(gameProfile.name, gameProfile.id)))
        return true
    }

    fun unregister(minecraftName: String): Boolean
    {
        return registeredUsers.removeAll { it.minecraft == getMinecraftUser(minecraftName) }
    }

    fun unregister(uuid: UUID): Boolean
    {
        return registeredUsers.removeAll { it.minecraft == getMinecraftUser(uuid) }
    }

    fun unregister(snowflake: Snowflake): Boolean
    {
        return registeredUsers.removeAll { it.discord == getDiscordUser(snowflake) }
    }
    // endregion

    // region config stuffs
    fun clearRegistered()
    {
        registeredUsers.clear()
    }

    fun register(userEntry: UserEntry)
    {
        val (snowflake, minecraftName) = userEntry
        Main.scope.launch {
            register(Snowflake(snowflake), minecraftName)
        }
    }

    fun getRegisteredUsersForConfig(): List<UserEntry>
    {
        return registeredUsers.map { UserEntry(it.discord.id.value.toLong(), it.minecraft.name) }
    }

    fun saveToCache(gameProfile: GameProfile)
    {
        if (userCache.none { it.name == gameProfile.name && it.uuid == gameProfile.id })
        {
            userCache.add(MinecraftUser(gameProfile.name, gameProfile.id))
        }
        minecraftProfiles.put(gameProfile)
        Main.scope.launch { saveCacheToFile() }

    }

    fun saveToFile()
    {
        Main.scope.launch {
            saveRegisteredUsersToFile()
            saveCacheToFile()
        }
    }

    fun saveRegisteredUsersToFile()
    {
        Config.configEntry.registeredUsers = getRegisteredUsersForConfig().toMutableList()
        Config.store()
    }

    suspend fun saveCacheToFile()
    {
        Config.configEntry.userCache = userCache.toMutableSet()
        Config.store()
    }
    // endregion

    fun User.prettyString(): String
    {
        val member = discordMembers[this.discord] ?: return "~not a member~ aka `${this.minecraft.name}`"
        return "${member.prettyName()} aka `${this.minecraft.name}`"
    }

    fun List<User>.onlyMinecraftNames(): List<String> =
        this.map { it.minecraft.name }

    fun loadGameProfilesFromPlayerData()
    {
        minecraftServer?.let { server ->
            // has to be this complicated, because user cache does not allow getting all profiles...
            Files.walk(server.getSavePath(WorldSavePath.PLAYERDATA)).toList()
                .asSequence()
                .map { it.fileName.toString() }
                .filter { it.endsWith(".dat") }
                .map { it.removeSuffix(".dat") }
                .mapNotNull {
                    try
                    {
                        UUID.fromString(it)
                    } catch (_: Exception)
                    {
                        null
                    }
                }
                .forEach { uuid ->
                    server.gameProfileRepo // this seems to do nothing (?)
                    server.userCache?.getByUuid(uuid)?.unwrap()?.let { profile ->
                        if (profile.isComplete) saveToCache(profile)
                    } ?: return@forEach
                }
        }
    }

    fun loadUserCache()
    {
        userCache = Config.configEntry.userCache
        userCache.forEach { minecraftUser ->
            if (minecraftProfiles.none { it.name.equals(minecraftUser.name, ignoreCase = true) })
            {
                findMinecraftProfileOrError(minecraftUser.name)
            }
        }
    }

    private fun findMinecraftProfileOrError(minecraftName: String): Boolean
    {
        var found = false

        minecraftServer?.gameProfileRepo?.findProfilesByNames(arrayOf(minecraftName), Agent.MINECRAFT, object : ProfileLookupCallback
        {
            override fun onProfileLookupSucceeded(profile: GameProfile?)
            {
                profile?.let {
                    if (profile.isComplete)
                    {
                        saveToCache(profile)
                        found = true
                        return
                    } else
                    {
                        logger.error("profile for $minecraftName not complete even though lookup succeeded")
                    }
                } ?: logger.error("profile for $minecraftName null even though lookup succeeded")
            }

            override fun onProfileLookupFailed(profile: GameProfile?, exception: java.lang.Exception?)
            {
                found = false
                logger.error("no profile found for $minecraftName")
            }
        })

        return found
    }

    private suspend fun findDiscordMemberOrError(snowflake: Snowflake): Boolean
    {
        discordMembers[DiscordUser(snowflake)] = DiscordHandler.getMemberOrSendError(snowflake) ?: return false
        return true
    }

    fun <V> MutableSet<V>.put(element: V)
    {
        this.remove(element)
        this.add(element)
    }

    suspend fun updateAllSkins()
    {
        userCache.forEach { it.getSkinURL() }

        saveToFile()
    }
}

