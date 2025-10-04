package de.jagenka

import com.mojang.authlib.GameProfile
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
import net.minecraft.server.PlayerConfigEntry
import net.minecraft.server.WhitelistEntry
import net.minecraft.util.Uuids
import net.minecraft.util.WorldSavePath
import java.nio.file.Files
import java.util.*
import kotlin.math.max
import kotlin.system.exitProcess

object UserRegistry
{
    private val registeredUsers = mutableSetOf<User>()

    private val discordMembers = mutableMapOf<DiscordUser, Member>()
    private var diskordelUserCache = mutableSetOf<MinecraftUser>()
    private val minecraftProfiles = GameProfileSet()

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
                minecraftProfiles.add(handler.player.gameProfile)
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
        return registeredUsers.find { it.minecraft.username.equals(minecraftName, ignoreCase = true) }
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
        return diskordelUserCache.find { it.username == name }
    }

    fun getMinecraftUser(uuid: UUID): MinecraftUser?
    {
        return diskordelUserCache.find { it.uuid == uuid }
    }

    fun getGameProfileFromMinecraft(name: String? = null, uuid: UUID? = null): GameProfile?
    {
        if (name != null)
        {
            minecraftServer?.apiServices?.profileResolver?.getProfileByName(name)?.unwrap()?.let {
                saveToCache(it)
                return it
            }
        }

        if (uuid != null)
        {
            minecraftServer?.apiServices?.profileResolver?.getProfileById(uuid)?.unwrap()?.let {
                saveToCache(it)
                return it
            }
        }

        // if no profiles can be found
        return null
    }

    fun getGameProfile(name: String, updateIfNecessary: Boolean = false): GameProfile?
    {
        val profile = minecraftProfiles.find { it.name.equals(name, ignoreCase = true) }

        if (updateIfNecessary && profile == null)
        {
            return getGameProfileFromMinecraft(name = name)
        }

        return profile
    }

    fun getGameProfile(uuid: UUID, updateIfNecessary: Boolean = false): GameProfile?
    {
        val profile = minecraftProfiles.find { it.id == uuid }

        if (updateIfNecessary && profile == null)
        {
            return getGameProfileFromMinecraft(uuid = uuid)
        }

        return profile
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
                    || possibleRegisteredUsers.find { user -> user.minecraft.username.equals(gameProfile.name, ignoreCase = true) } != null
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
                getGameProfile(minecraftName, true)
                    ?: GameProfile(
                        diskordelUserCache.find { it.username.equals(minecraftName, ignoreCase = true) }?.uuid
                            ?: minecraftServer?.apiServices?.profileRepository?.findProfileByName(minecraftName)?.unwrap()?.id
                            ?: Uuids.getOfflinePlayerUuid(minecraftName),
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
    fun loadRegisteredUsersFromDiskordelConfig()
    {
        if (minecraftServer?.playerManager?.whitelist == null)
        {
            logger.error("cannot load registered users from diskordel config, as whitelist is not initialized yet")
            exitProcess(69)
        }

        // clear first
        clearRegisteredWithDiskordel()

        // register and whitelist all legal users from Diskordel config
        Config.configEntry.registeredUsers.toList().forEach { (snowflakeId, minecraftName) ->
            // try to register with diskordel
            register(Snowflake(snowflakeId), minecraftName) { success ->
                if (success)
                {
                    val profile = getGameProfile(minecraftName, true)

                    if (profile == null)
                    {
                        logger.error("could not find profile for $minecraftName while registering user from diskordel config. they will not be whitelisted")
                        unregister(minecraftName)
                        return@register
                    }

                    // not in whitelist, but should be on
                    if (minecraftServer?.playerManager?.whitelist?.isAllowed(PlayerConfigEntry(profile)) == false)
                    {
                        minecraftServer?.playerManager?.whitelist?.add(WhitelistEntry(PlayerConfigEntry(profile)))
                        logger.info("whitelisted $minecraftName")
                    }
                } else
                {
                    logger.info("$minecraftName will not be whitelisted, as they cannot be found in Discord guild")
                }

            }
        }

        // remove all users from whitelist that are not legal in Diskordel config
        minecraftServer?.playerManager?.whitelist?.values()?.toList()?.forEach {
            val nameInWhitelist = it.key?.name ?: return@forEach

            if (nameInWhitelist !in Config.configEntry.registeredUsers.map { userInConfig -> userInConfig.minecraftName })
            {
                minecraftServer?.playerManager?.whitelist?.remove(it)
            }
        }
    }

    fun clearRegisteredWithDiskordel()
    {
        registeredUsers.clear()
    }

    fun getRegisteredUsersForConfig(): List<UserEntry>
    {
        return registeredUsers.map { UserEntry(it.discord.id.value.toLong(), it.minecraft.username) }
    }

    fun saveToCache(gameProfile: GameProfile)
    {
        val user = MinecraftUser(gameProfile.name, gameProfile.id)
        diskordelUserCache.find { it.uuid == gameProfile.id }?.apply {
            this.username = gameProfile.name
        }
            ?: diskordelUserCache.find { it.username.equals(gameProfile.name, ignoreCase = true) }?.apply {
                this.username = gameProfile.name
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
                "   aka   ${this.minecraft.username}"
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
                minecraftName = it.minecraft.username
            )
        }
    }

    /**
     * check if UUIDs from files are in Minecraft's user cache (as provided by api services profile resolver), and import that data accordingly
     */
    fun loadGameProfilesToCacheFromMinecraftServicesWithAvailableFiles()
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
                    saveToCache(minecraftServer?.apiServices?.profileResolver()?.getProfileById(uuid)?.unwrap() ?: return@forEach)
                }
        }
    }

    /**
     * load local cache with locally saved names - updates to names will be detected on login
     */
    fun loadDiskordelUserCache()
    {
        Config.configEntry.userCache.toMutableSet().forEach { userFromConfig ->
            minecraftProfiles.add(GameProfile(userFromConfig.uuid, userFromConfig.username))
            diskordelUserCache.put(MinecraftUser(userFromConfig.username, userFromConfig.uuid, userFromConfig.skinURL, userFromConfig.lastURLUpdate))
            val name = userFromConfig.username.lowercase()
            precomputeName(name, name)
        }
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

