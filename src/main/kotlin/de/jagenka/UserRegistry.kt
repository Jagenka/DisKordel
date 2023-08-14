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
import net.minecraft.server.WhitelistEntry
import net.minecraft.util.Uuids
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

    fun findUser(snowflake: Snowflake?): User?
    {
        if (snowflake == null) return null
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
    fun register(snowflake: Snowflake, minecraftName: String, callback: (success: Boolean) -> Unit = {})
    {
        findDiscordMember(snowflake)
        val gameProfile =
            getGameProfile(minecraftName)
                ?: findMinecraftProfileOrError(minecraftName)
                ?: minecraftServer?.playerManager?.whitelist?.values()?.map { it.key }?.find { it?.name?.equals(minecraftName, ignoreCase = true) ?: false }
                ?: GameProfile(
                    userCache.find { it.name.equals(minecraftName, ignoreCase = true) }?.uuid ?: Uuids.getOfflinePlayerUuid(minecraftName),
                    minecraftName
                )
        registeredUsers.put(User(discord = DiscordUser(snowflake), minecraft = MinecraftUser(gameProfile.name, gameProfile.id)))
        saveToCache(gameProfile)

        callback.invoke(true)
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
    fun loadRegisteredUsersFromFile()
    {
        clearRegistered()
        findMinecraftProfilesOrError(Config.configEntry.registeredUsers.map { it.minecraftName }
            .filter { !userCache.map { it.name.lowercase() }.contains(it.lowercase()) }
            .toList())
        Config.configEntry.registeredUsers.forEach { register(it) }

        minecraftServer?.playerManager?.whitelist?.apply {
            // remove players from whitelist, if they are not registered
            this.values().toList().forEach { onWhitelist ->
                if (onWhitelist.key?.id !in registeredUsers.map { it.minecraft.uuid })
                {
                    this.remove(onWhitelist)
                }
            }
            // add players to whitelist if not already happened
            registeredUsers.forEach { inRegistry ->
                val gameProfile = getGameProfile(inRegistry.minecraft.uuid) ?: return@forEach
                if (!this.isAllowed(gameProfile))
                {
                    this.add(WhitelistEntry(gameProfile))
                }
            }
        }
    }

    fun clearRegistered()
    {
        registeredUsers.clear()
    }

    fun register(userEntry: UserEntry)
    {
        val (snowflake, minecraftName) = userEntry
        register(Snowflake(snowflake), minecraftName)
    }

    fun getRegisteredUsersForConfig(): List<UserEntry>
    {
        return registeredUsers.map { UserEntry(it.discord.id.value.toLong(), it.minecraft.name) }
    }

    fun saveToCache(gameProfile: GameProfile)
    {
        val user = MinecraftUser(gameProfile.name, gameProfile.id)
        userCache.find { it.uuid == gameProfile.id }?.apply {
            this.name = gameProfile.name
        }
            ?: userCache.find { it.name.equals(gameProfile.name, ignoreCase = true) }?.apply {
                this.name = gameProfile.name
                this.uuid = gameProfile.id
            }
            ?: userCache.add(user)

        minecraftProfiles.add(gameProfile)
        saveCacheToFile()
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

    fun saveCacheToFile()
    {
        userCache.addAll(minecraftProfiles.map { MinecraftUser(it.name, it.id) })
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
        // filter is, so that no double-request is made
        val foundProfiles = findMinecraftProfilesOrError(Config.configEntry.userCache.toMutableSet().filter { it !in this.userCache }.map { it.name })

        Config.configEntry.userCache.toMutableSet().forEach { userFromConfig ->
            val gameProfile = foundProfiles.find { it.id == userFromConfig.uuid } ?: return@forEach
            userCache.put(MinecraftUser(gameProfile.name, gameProfile.id, userFromConfig.skinURL, userFromConfig.lastURLUpdate))
        }
    }

    private fun findMinecraftProfilesOrError(names: List<String>): List<GameProfile>
    {
        if (names.isEmpty()) return emptyList()

        val result = mutableListOf<GameProfile>()

        try
        {
            minecraftServer?.gameProfileRepo?.findProfilesByNames(names.shuffled().toTypedArray(), Agent.MINECRAFT, object : ProfileLookupCallback
            {
                override fun onProfileLookupSucceeded(profile: GameProfile?)
                {
                    profile?.let {
                        if (profile.isComplete)
                        {
                            minecraftProfiles.add(profile)
                            result.add(profile)
                            return
                        } else
                        {
                            logger.error("profile for ${profile.name} not complete even though lookup succeeded")
                        }
                    } ?: logger.error("profile null even though lookup succeeded")
                }

                override fun onProfileLookupFailed(profile: GameProfile?, exception: java.lang.Exception?)
                {
                    logger.error("no profile found for ${profile?.name}")
                }
            })
        } catch (e: Exception)
        {
            logger.error("error finding game profiles for $names")
        }

        return result.toList()
    }

    private fun findMinecraftProfileOrError(minecraftName: String): GameProfile?
    {
        var found = false

        try
        {
            minecraftServer?.gameProfileRepo?.findProfilesByNames(arrayOf(minecraftName), Agent.MINECRAFT, object : ProfileLookupCallback
            {
                override fun onProfileLookupSucceeded(profile: GameProfile?)
                {
                    profile?.let {
                        if (profile.isComplete)
                        {
                            minecraftProfiles.add(profile)
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
        } catch (e: Exception)
        {
            logger.error("error finding game profile for $minecraftName")
        }

        return if (found) getGameProfile(minecraftName) else null
    }

    private fun findDiscordMember(snowflake: Snowflake)
    {
        Main.scope.launch { discordMembers[DiscordUser(snowflake)] = DiscordHandler.getMemberOrSendError(snowflake) ?: return@launch }
    }

    fun <V> MutableSet<V>.put(element: V)
    {
        this.remove(element)
        this.add(element)
    }
}

