package de.jagenka

import com.mojang.authlib.GameProfile
import com.mojang.authlib.ProfileLookupCallback
import de.jagenka.DiscordHandler.asCodeBlock
import de.jagenka.DiscordHandler.kord
import de.jagenka.DiscordHandler.prettyName
import de.jagenka.MinecraftHandler.logger
import de.jagenka.MinecraftHandler.minecraftServer
import de.jagenka.Util.code
import de.jagenka.Util.unwrap
import de.jagenka.config.Config
import de.jagenka.config.UserEntry
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Member
import dev.kord.core.entity.effectiveName
import info.debatty.java.stringsimilarity.Levenshtein
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
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
    private var diskordelUserCache = mutableSetOf<MinecraftUser>()
    private val minecraftProfiles = mutableSetOf<GameProfile>()

    private val discordUserCache = mutableSetOf<dev.kord.core.entity.User>()

    /**
     * some name to their Minecraft name
     */
    private val nameToMinecraftName = mutableMapOf<String, String>()
    private val comparator = Levenshtein()

    init
    {
        // on player join, save their GameProfile
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            Main.scope.launch {
                saveToCache(handler.player.gameProfile)
            }
        }
    }

    fun precomputeName(input: String, minecraftName: String)
    {
        nameToMinecraftName[input] = minecraftName
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
        return diskordelUserCache.find { it.name == name }
    }

    fun getMinecraftUser(uuid: UUID): MinecraftUser?
    {
        return diskordelUserCache.find { it.uuid == uuid }
    }

    fun getGameProfile(name: String): GameProfile?
    {
        return minecraftProfiles.find { it.name.equals(name, ignoreCase = true) }
    }

    fun getGameProfile(uuid: UUID?): GameProfile?
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
        val registeredUsers = getAllRegisteredUsers()
        val usersListString = registeredUsers.getPrettyUsersList()
        return "# There are currently ${registeredUsers.size.toString().code()} users registered:\n" + usersListString.asCodeBlock()
    }

    fun getMinecraftProfiles() = minecraftProfiles.toSet()

    fun findRegistered(name: String?): List<User>
    {
        return registeredUsers.filter {
            it.isLikely(name ?: "")
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

    fun findMostLikelyMinecraftName(input: String): String
    {
        return nameToMinecraftName.minBy { (someName, _) ->
            comparator.distance(input, someName)
        }.value
    }
    // endregion

    fun updateDiscordName(snowflake: Snowflake)
    {
        registeredUsers.find { it.discord.id == snowflake } ?: return // if not in registry, ignore this one
        findDiscordMember(snowflake) { /* noting to do */ }
    }

    fun updateDiscordName(member: Member)
    {
        registeredUsers.find { it.discord.id == member.id } ?: return // if not in registry, ignore this one
        discordMembers[DiscordUser(member.id)] = member
    }

    // region registration
    fun register(snowflake: Snowflake, minecraftName: String, callback: (success: Boolean) -> Unit = {})
    {
        // check if user is even on this Discord Guild
        findDiscordMember(snowflake = snowflake, failureCallback = { callback.invoke(false) }) { member ->
            val gameProfile =
                getGameProfile(minecraftName)
                    ?: minecraftServer?.playerManager?.whitelist?.values()?.map { it.key }?.find { it?.name?.equals(minecraftName, ignoreCase = true) == true }
                    ?: findMinecraftProfileOrError(minecraftName)
                    ?: GameProfile(
                        diskordelUserCache.find { it.name.equals(minecraftName, ignoreCase = true) }?.uuid ?: Uuids.getOfflinePlayerUuid(minecraftName),
                        minecraftName
                    )

            saveToCache(gameProfile)
            registeredUsers.put(User(discord = DiscordUser(member.id), minecraft = MinecraftUser(gameProfile.name, gameProfile.id)))

            val gameProfileName = gameProfile.name.lowercase()
            precomputeName(member.effectiveName, gameProfileName)
            precomputeName(member.username, gameProfileName)

            callback.invoke(true)
        }
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
        // search for UUIDs, if not in local user cache
        findMinecraftProfilesOrError(Config.configEntry.registeredUsers.map { it.minecraftName }
            .filter { !diskordelUserCache.map { it.name.lowercase() }.contains(it.lowercase()) }
            .toList())

        // remove all players from whitelist
        minecraftServer?.playerManager?.whitelist?.apply {
            this.values().toList().forEach { onWhitelist ->
                this.remove(onWhitelist)
            }
        }
        // register and whitelist all legal users
        Config.configEntry.registeredUsers.toList().forEach { (id, name) ->
            register(Snowflake(id), name) { success ->
                if (success)
                {
                    minecraftServer?.playerManager?.whitelist?.apply {
                        val gameProfile = getGameProfile(name) ?: return@register
                        if (!this.isAllowed(gameProfile))
                        {
                            this.add(WhitelistEntry(gameProfile))
                        }
                    }
                } else
                {
                    logger.info("$name will not be whitelisted.")
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
        diskordelUserCache.find { it.uuid == gameProfile.id }?.apply {
            this.name = gameProfile.name
        }
            ?: diskordelUserCache.find { it.name.equals(gameProfile.name, ignoreCase = true) }?.apply {
                this.name = gameProfile.name
                this.uuid = gameProfile.id
            }
            ?: diskordelUserCache.add(user)

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
        Config.configEntry.userCache = diskordelUserCache.toMutableSet()
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
                    " " + minecraftName
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

    /**
     * check if UUIDs from files are in Minecraft's user cache, and import that data accordingly
     */
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
                    server.userCache?.getByUuid(uuid)?.unwrap()?.let { profile ->
                        saveToCache(profile)
                    } ?: return@forEach
                }
        }
    }

    /**
     * load local cache with locally saved names - updates to names will be detected on login
     */
    fun loadUserCache()
    {
        Config.configEntry.userCache.toMutableSet().forEach { userFromConfig ->
            diskordelUserCache.put(MinecraftUser(userFromConfig.name, userFromConfig.uuid, userFromConfig.skinURL, userFromConfig.lastURLUpdate))
            val name = userFromConfig.name.lowercase()
            precomputeName(name, name)
        }
    }

    private fun findMinecraftProfilesOrError(names: List<String>): List<GameProfile>
    {
        if (names.isEmpty()) return emptyList()

        val result = mutableListOf<GameProfile>()

        try
        {
            logger.info("finding game profiles for $names")
            minecraftServer?.gameProfileRepo?.findProfilesByNames(names.shuffled().toTypedArray(), object : ProfileLookupCallback
            {
                override fun onProfileLookupSucceeded(profile: GameProfile?)
                {
                    profile?.let {
                        saveToCache(profile)
                        result.add(profile)
                        return
                    } ?: logger.error("profile null even though lookup succeeded")
                }

                override fun onProfileLookupFailed(profileName: String?, exception: java.lang.Exception?)
                {
                    logger.error("no profile found for $profileName while looking for multiple profiles")
                }
            })
        } catch (e: Exception)
        {
            logger.error("error finding game profiles for $names while looking for multiple profiles")
        }

        return result.toList()
    }

    private fun findMinecraftProfileOrError(minecraftName: String): GameProfile?
    {
        var found = false

        try
        {
            logger.info("finding game profile for $minecraftName")
            minecraftServer?.gameProfileRepo?.findProfilesByNames(arrayOf(minecraftName), object : ProfileLookupCallback
            {
                override fun onProfileLookupSucceeded(profile: GameProfile?)
                {
                    profile?.let {
                        saveToCache(profile)
                        found = true
                        return
                    } ?: logger.error("profile for $minecraftName null even though lookup succeeded")
                }

                override fun onProfileLookupFailed(profileName: String?, exception: java.lang.Exception?)
                {
                    found = false
                    logger.error("no profile found for $minecraftName while looking for one profile")
                }
            })
        } catch (e: Exception)
        {
            logger.error("error finding game profile for $minecraftName while looking for one profile")
        }

        return if (found) getGameProfile(minecraftName) else null
    }

    fun removeDiscordMember(snowflake: Snowflake)
    {
        discordMembers.remove(DiscordUser(snowflake))
    }

    private fun findDiscordMember(snowflake: Snowflake, failureCallback: () -> Unit = {}, successCallback: (member: Member) -> Unit)
    {
        Main.scope.launch {
            val member = DiscordHandler.getMemberOrSendError(snowflake)
            if (member != null)
            {
                discordMembers[DiscordUser(snowflake)] = member
                successCallback.invoke(member)
            } else
            {
                failureCallback.invoke()
            }
        }
    }

    fun <V> MutableSet<V>.put(element: V)
    {
        this.remove(element)
        this.add(element)
    }
}

data class UserName(val displayName: String, val username: String, val minecraftName: String)

