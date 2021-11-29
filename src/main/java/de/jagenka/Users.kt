package de.jagenka

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient

class Users : BiMap<Snowflake, String>()
{
    fun registerUser(userId: Snowflake, minecraftName: String): Boolean
    {
        if (this.containsValue(minecraftName)) return false

        this.put(userId, minecraftName)

        return true
    }

    fun getDiscordMember(inputName: String, gateway: GatewayDiscordClient, guildId: Snowflake): Snowflake?
    {
        keys().forEach {
            val member = gateway.getMemberById(guildId, it).block()
            if (member != null)
            {
                if (member.username == inputName) return it
                if (member.displayName == inputName) return it
            }
        }
        return null
    }

    fun containsDiscordMember(displayName: String, gateway: GatewayDiscordClient, guildId: Snowflake): Boolean
    {
        val discordMember = getDiscordMember(displayName, gateway, guildId)
        return discordMember != null

    }

    fun getConfigList(): List<UsersConfigEntry>
    {
        val arrayList = ArrayList<UsersConfigEntry>()
        keys().forEach { arrayList.add(UsersConfigEntry(it.asLong(), getValueForKey(it).orEmpty())) }
        return arrayList
    }

    fun getAsUserConfigSet(): Set<UsersConfigEntry>
    {
        val set = HashSet<UsersConfigEntry>()
        keys().forEach { set.add(UsersConfigEntry(it.asLong(), getValueForKey(it).orEmpty())) }
        return set
    }
}