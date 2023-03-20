package de.jagenka

import com.mojang.authlib.Agent
import com.mojang.authlib.GameProfile
import com.mojang.authlib.ProfileLookupCallback
import de.jagenka.MinecraftHandler.logger
import de.jagenka.MinecraftHandler.minecraftServer
import de.jagenka.Util.unwrap
import de.jagenka.config.Config
import de.jagenka.config.UserEntry
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Member
import net.minecraft.util.WorldSavePath
import java.nio.file.Files
import java.util.*

object UserRegistry
{
    private val users = mutableSetOf<User>()

    private val discordMembers = mutableMapOf<DiscordUser, Member>()
    private val minecraftProfiles = mutableSetOf<GameProfile>()

    // region getter
    fun getMinecraftUser(name: String): MinecraftUser?
    {
        return users.find { it.minecraft.name.equals(name, ignoreCase = true) }?.minecraft
    }

    fun getMinecraftUser(uuid: UUID): MinecraftUser?
    {
        return users.find { it.minecraft.uuid == uuid }?.minecraft
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
        return users.find { it.discord.id == id }?.discord
    }

    fun getDiscordMember(snowflake: Snowflake): Member?
    {
        return discordMembers[DiscordUser(snowflake)]
    }

    fun getDiscordMember(inputName: String): Member?
    {
        return discordMembers.values.find { it.username == inputName || it.displayName == inputName }
    }
    // endregion

    // region registration
    suspend fun register(snowflake: Snowflake, minecraftName: String): Boolean
    {
        if (!findMinecraftProfile(minecraftName)) return false
        if (!findDiscordMember(snowflake)) return false

        users.add(User(DiscordUser(snowflake), MinecraftUser(minecraftName, getGameProfile(minecraftName)?.id ?: return false)))
        return true
    }

    fun unregister(minecraftName: String): Boolean
    {
        return users.removeAll { it.minecraft == getMinecraftUser(minecraftName) }
    }

    fun unregister(uuid: UUID): Boolean
    {
        return users.removeAll { it.minecraft == getMinecraftUser(uuid) }
    }

    fun unregister(snowflake: Snowflake): Boolean
    {
        return users.removeAll { it.discord == getDiscordUser(snowflake) }
    }
    // endregion

    // region config stuffs
    fun getForConfig(): List<UserEntry>
    {
        return users.map { UserEntry(it.discord.id.value.toLong(), it.minecraft.name, it.minecraft.uuid.toString()) }
    }

    fun saveToFile()
    {
        Config.configEntry.users = getForConfig()
        Config.store()
    }
    // endregion

    fun getAllUsers() = users.toList()

    fun find(name: String): List<User>
    {
        return users.filter {
            discordMembers[it.discord]?.username?.contains(name, ignoreCase = true) ?: false
                    || discordMembers[it.discord]?.displayName?.contains(name, ignoreCase = true) ?: false
                    || it.minecraft.name.contains(name, ignoreCase = true)
        }
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
                    server.userCache.getByUuid(uuid).unwrap()?.let { profile ->
                        if (profile.isComplete) minecraftProfiles.add(profile)
                    } ?: return@forEach
                }
        }
    }

    private fun findMinecraftProfile(minecraftName: String): Boolean
    {
        var found = false

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

        return found
    }

    private suspend fun findDiscordMember(snowflake: Snowflake): Boolean
    {
        discordMembers[DiscordUser(snowflake)] = DiscordHandler.guild.getMemberOrNull(snowflake) ?: return false
        return true
    }
}

