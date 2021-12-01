package de.jagenka

import discord4j.core.`object`.entity.Member

class Users : BiMap<Member, String>()
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

    fun getAsUsersConfigList(): List<UsersConfigEntry>
    {
        val arrayList = ArrayList<UsersConfigEntry>()
        keys().forEach { arrayList.add(UsersConfigEntry(it.id.asLong(), getValueForKey(it).orEmpty())) }
        return arrayList
    }

    fun getAsWhoIsOutputList(): List<WhoIsOutput>
    {
        val list = ArrayList<WhoIsOutput>()
        keys().forEach { list.add(WhoIsOutput(it.username, it.displayName, getValueForKey(it).orEmpty())) }
        return list
    }

    fun getAsUserConfigSet(): Set<UsersConfigEntry>
    {
        val set = HashSet<UsersConfigEntry>()
        keys().forEach { set.add(UsersConfigEntry(it.id.asLong(), getValueForKey(it).orEmpty())) }
        return set
    }

    fun find(name: String): List<WhoIsOutput>
    {
        val list = ArrayList<WhoIsOutput>()
        keys().forEach {
            val username = it.username
            val displayName = it.displayName
            val minecraftName = getValueForKey(it).orEmpty()

            if (username.contains(name, ignoreCase = true) || displayName.contains(name, ignoreCase = true) || minecraftName.contains(name, ignoreCase = true))
            {
                list.add(WhoIsOutput(username, displayName, minecraftName))
            }
        }
        return list
    }
}

data class WhoIsOutput(val username: String, val displayName: String, val minecraftName: String)