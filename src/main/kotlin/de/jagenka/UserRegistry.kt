package de.jagenka

import com.mojang.authlib.GameProfile
import com.mojang.authlib.ProfileLookupCallback
import de.jagenka.DiscordHandler.kord
import de.jagenka.DiscordHandler.prettyName
import de.jagenka.MinecraftHandler.logger
import de.jagenka.MinecraftHandler.minecraftServer
import de.jagenka.Util.unwrap
import de.jagenka.config.Config
import de.jagenka.config.UserEntry
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Member
import dev.kord.core.entity.effectiveName
import info.debatty.java.stringsimilarity.Levenshtein
import kotlinx.coroutines.launch
import net.minecraft.server.WhitelistEntry
import net.minecraft.util.Uuids
import net.minecraft.util.WorldSavePath
import java.nio.file.Files
import java.util.*
import kotlin.math.max

object UserRegistry
{
    private val registeredUsers = mutableSetOf<User>()

    private val discordMembers = mutableMapOf<DiscordUser, Member>()
    private var userCache = mutableSetOf<MinecraftUser>()
    private val minecraftProfiles = mutableSetOf<GameProfile>()

    private val discordUserCache = mutableSetOf<dev.kord.core.entity.User>()

    /**
     * some name to their Minecraft name
     */
    private val nameToMinecraftName = mutableMapOf<String, String>()
    private val comparator = Levenshtein()

    fun precomputeName(input: String, minecraftName: String)
    {
        nameToMinecraftName[input] = minecraftName
    }

    fun prepareNamesForComparison()
    {
        userCache.forEach {
            val name = it.name.lowercase()
            precomputeName(name, name)
        }

        minecraftProfiles.forEach {
            val name = it.name.lowercase()
            precomputeName(name, name)
        }

        registeredUsers.forEach { user ->
            discordMembers[user.discord]?.effectiveName?.let { precomputeName(it, user.minecraft.name) }
            discordMembers[user.discord]?.username?.let { precomputeName(it, user.minecraft.name) }
            precomputeName(user.minecraft.name, user.minecraft.name)
        }
    }

    // region getter
    fun getDiscordMembers(): Map<DiscordUser, Member>
    {
        return discordMembers.toMap()
    }

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

    suspend fun getAllUsersAsOutput(): String
    {
        return "Currently registered Users:\n\n" + getAllRegisteredUsers().getPrettyUsersList()
    }

    fun getMinecraftProfiles() = minecraftProfiles.toSet()

    fun findRegistered(name: String): List<User>
    {
        return registeredUsers.filter {
            it.isLikely(name)
        }
    }

    fun findMinecraftProfiles(name: String): List<GameProfile>
    {
        val possibleRegisteredUsers = findRegistered(name)
        return minecraftProfiles.filter { gameProfile ->
            gameProfile.name.contains(name, ignoreCase = true)
                    || possibleRegisteredUsers.find { user -> user.minecraft.name.equals(gameProfile.name, ignoreCase = true) } != null
        }
    }

    fun findMostLikelyMinecraftName(input: String): String?
    {
        return nameToMinecraftName.minBy { (someName, _) ->
            comparator.distance(input, someName)
        }.value
    }
    // endregion

    // region registration
    fun register(snowflake: Snowflake, minecraftName: String, callback: (success: Boolean) -> Unit = {})
    {
        val gameProfile =
            getGameProfile(minecraftName)
                ?: findMinecraftProfileOrError(minecraftName)
                ?: minecraftServer?.playerManager?.whitelist?.values()?.map { it.key }?.find { it?.name?.equals(minecraftName, ignoreCase = true) ?: false }
                ?: GameProfile(
                    userCache.find { it.name.equals(minecraftName, ignoreCase = true) }?.uuid ?: Uuids.getOfflinePlayerUuid(minecraftName),
                    minecraftName
                )
        registeredUsers.put(User(discord = DiscordUser(snowflake), minecraft = MinecraftUser(gameProfile.name, gameProfile.id)))
        val gameProfileName = gameProfile.name.lowercase()
        findDiscordMember(snowflake) {
            precomputeName(it.effectiveName, gameProfileName)
            precomputeName(it.username, gameProfileName)
        }
        precomputeName(gameProfileName, gameProfileName)
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
        val name = gameProfile.name.lowercase()
        precomputeName(name, name)
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
        minecraftProfiles.forEach {
            val name = it.name.lowercase()
            precomputeName(name, name)
        }
        Config.configEntry.userCache = userCache.toMutableSet()
        Config.store()
    }
    // endregion

    fun User.prettyString(): String
    {
        return (discordMembers[this.discord]?.prettyName() ?: "~not a member~") +
                "   aka   ${this.minecraft.name}"
    }

    /**
     * this method assumes equal character width
     */
    suspend fun List<User>.getPrettyUsersList(): String
    {
        val userNames = this.getUserNames()

        val displayNameHeader = "Display Name"
        val displayNameColWidth = max(userNames.maxOfOrNull { it.displayName.length } ?: 0, displayNameHeader.length) + 2

        val usernameHeader = "Username"
        val usernameColWidth = max(userNames.maxOfOrNull { it.username.length } ?: 0, usernameHeader.length) + 2

        val minecraftNameHeader = "Minecraft Name"
        val minecraftNameColWidth = max(userNames.maxOfOrNull { it.minecraftName.length } ?: 0, minecraftNameHeader.length) + 2

        var header = displayNameHeader.padEnd(displayNameColWidth + 1, ' ') +
                usernameHeader.padEnd(usernameColWidth + 1, ' ') +
                minecraftNameHeader.padEnd(minecraftNameColWidth + 1, ' ')
        header += "\n" + "-".repeat(header.length) + "\n"

        return userNames.joinToString(prefix = header, separator = "\n") { (displayName, username, minecraftName) ->
            " " + displayName.padEnd(displayNameColWidth - 1, ' ') + "|" +
                    " " + username.padEnd(usernameColWidth - 1, ' ') + "|" +
                    " " + minecraftName.padEnd(minecraftNameColWidth - 1, ' ')
        }
    }

    suspend fun List<User>.getUserNames(): List<UserName>
    {
        return this.map {
            val discordUser = discordMembers[it.discord] ?: discordUserCache.find { cachedUser -> cachedUser.id == it.discord.id } ?: kord?.getUser(it.discord.id)
            if (discordUser != null)
            {
                discordUserCache.add(discordUser)
            }
            UserName(
                displayName = (discordUser as? Member)?.effectiveName ?: discordUser?.effectiveName ?: "noname",
                username = (discordUser as? Member)?.username ?: discordUser?.username ?: "noname",
                minecraftName = it.minecraft.name
            )
        }
    }

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
                        saveToCache(profile)
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
            val name = gameProfile.name.lowercase()
            precomputeName(name, name)
        }
    }

    private fun findMinecraftProfilesOrError(names: List<String>): List<GameProfile>
    {
        if (names.isEmpty()) return emptyList()

        val result = mutableListOf<GameProfile>()

        try
        {
            minecraftServer?.gameProfileRepo?.findProfilesByNames(names.shuffled().toTypedArray(), object : ProfileLookupCallback
            {
                override fun onProfileLookupSucceeded(profile: GameProfile?)
                {
                    profile?.let {
                        minecraftProfiles.add(profile)
                        val name = profile.name.lowercase()
                        precomputeName(name, name)
                        result.add(profile)
                        return
                    } ?: logger.error("profile null even though lookup succeeded")
                }

                override fun onProfileLookupFailed(profileName: String?, exception: java.lang.Exception?)
                {
                    logger.error("no profile found for $profileName")
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
            minecraftServer?.gameProfileRepo?.findProfilesByNames(arrayOf(minecraftName), object : ProfileLookupCallback
            {
                override fun onProfileLookupSucceeded(profile: GameProfile?)
                {
                    profile?.let {
                        minecraftProfiles.add(profile)
                        val name = profile.name.lowercase()
                        precomputeName(name, name)
                        found = true
                        return
                    } ?: logger.error("profile for $minecraftName null even though lookup succeeded")
                }

                override fun onProfileLookupFailed(profileName: String?, exception: java.lang.Exception?)
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

    private fun findDiscordMember(snowflake: Snowflake, callback: (mamber: Member) -> Unit)
    {
        Main.scope.launch {
            val member = DiscordHandler.getMemberOrSendError(snowflake)
            discordMembers[DiscordUser(snowflake)] = member ?: return@launch
            callback.invoke(member)
        }
    }

    fun <V> MutableSet<V>.put(element: V)
    {
        this.remove(element)
        this.add(element)
    }
}

data class UserName(val displayName: String, val username: String, val minecraftName: String)

