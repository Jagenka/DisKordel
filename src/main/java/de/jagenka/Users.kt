package de.jagenka

import discord4j.common.util.Snowflake
import discord4j.core.GatewayDiscordClient

class Users : BiMap<Snowflake, String>()
{
    fun getDiscordMember(displayName: String, gateway: GatewayDiscordClient, guildId: Snowflake): Snowflake?
    {
        keys().forEach {
            val member = gateway.getMemberById(guildId, it).block()
            if(member != null) if (member.displayName == displayName) return it
        }
        return null
    }

    fun containsDiscordMember(displayName: String, gateway: GatewayDiscordClient, guildId: Snowflake): Boolean
    {
        val discordMember = getDiscordMember(displayName, gateway, guildId)
        return discordMember != null

    }
}