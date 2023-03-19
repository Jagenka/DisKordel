package de.jagenka

import de.jagenka.MinecraftHandler.minecraftServer
import de.jagenka.config.Config
import de.jagenka.config.UserEntry
import dev.kord.core.entity.Member
import net.minecraft.util.WorldSavePath
import java.nio.file.Files
import java.util.*

object Users : BiMap<Member, String>()
{
    fun registerUser(member: Member, minecraftName: String)
    {
        this.put(member, minecraftName)
    }

    fun unregisterUser(member: Member)
    {
        this.removeKey(member)
    }

    fun getDiscordMember(inputName: String): Member?
    {
        keys().forEach {
            if (it.username == inputName) return it
            if (it.displayName == inputName) return it
        }
        return null
    }

    fun containsDiscordMember(inputName: String): Boolean
    {
        val discordMember = getDiscordMember(inputName)
        return discordMember != null
    }

    fun getAsUserEntryList(): List<UserEntry>
    {
        val list = mutableListOf<UserEntry>()
        keys().forEach { list.add(UserEntry(it.id.value.toLong(), getValueForKey(it).orEmpty())) }
        return list
    }

    fun getAsUserList(): List<User>
    {
        val list = ArrayList<User>()
        keys().forEach { list.add(User(it.username, it.displayName, getValueForKey(it).orEmpty())) }
        return list
    }

    fun getAsUserEntrySet(): Set<UserEntry>
    {
        val set = mutableSetOf<UserEntry>()
        keys().forEach { set.add(UserEntry(it.id.value.toLong(), getValueForKey(it).orEmpty())) }
        return set
    }

    fun find(name: String): List<User>
    {
        val list = ArrayList<User>()
        keys().forEach {
            val username = it.username
            val displayName = it.displayName
            val minecraftName = getValueForKey(it).orEmpty()

            if (name.isBlank() || username.contains(name, ignoreCase = true) || displayName.contains(name, ignoreCase = true) || minecraftName.contains(name, ignoreCase = true))
            {
                list.add(User(username, displayName, minecraftName))
            }
        }
        return list
    }

    fun List<User>.onlyMinecraftNames(): List<String>
    {
        val minecraftNames = ArrayList<String>()
        this.forEach { minecraftNames.add(it.minecraftName) }
        return minecraftNames
    }

    fun whoIsPrintable(name: String): String
    {
        val members = find(name)
        return if (members.isEmpty())
        {
            "No-one found!"
        } else
        {
            val sb = StringBuilder("")
            members.forEach {
                sb.append(it.prettyComboName)
                sb.appendLine()
            }
            sb.setLength(sb.length - 1)
            sb.toString()
        }
    }

    fun getAllKnownMinecraftUsers(): List<MinecraftUser>
    {
        val users = Config.configEntry.users.mapNotNull {
            if (it.uuid.isBlank()) return@mapNotNull null
            MinecraftUser(name = it.minecraftName.ifBlank { it.uuid }, uuid = UUID.fromString(it.uuid))
        }.toMutableList()

        users.addAll(minecraftServer?.let { server ->
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
                .map { MinecraftUser(name = PlayerStatManager.getPlayerNameFromUUID(it) ?: it.toString(), uuid = it) }
                .filter { fromPlayerData -> users.none { it.name.equals(fromPlayerData.name, ignoreCase = true) } }
        } ?: emptySequence())

        return users
    }

    fun saveToFile()
    {
        Config.configEntry.users = getAsUserEntryList().toList()
        Config.store()
    }
}

