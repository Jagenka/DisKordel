package de.jagenka

import de.jagenka.config.UserEntry
import dev.kord.core.entity.Member

object Users : BiMap<Member, String>()
{
    fun registerUser(member: Member, minecraftName: String): Boolean
    {
        if (this.containsValue(minecraftName)) return false

        this.put(member, minecraftName)

        return true
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

            if (username.contains(name, ignoreCase = true) || displayName.contains(name, ignoreCase = true) || minecraftName.contains(name, ignoreCase = true))
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
        val members = Users.find(name)
        return if (members.isEmpty())
        {
            "No users found!"
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
}

