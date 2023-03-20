package de.jagenka

import dev.kord.common.entity.Snowflake
import java.util.*

data class User(val discord: DiscordUser, val minecraft: MinecraftUser)
{
    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (discord != other.discord) return false

        return true
    }

    override fun hashCode(): Int
    {
        return discord.hashCode()
    }
}

data class MinecraftUser(val name: String, val uuid: UUID)
{
    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MinecraftUser

        if (uuid != other.uuid) return false

        return true
    }

    override fun hashCode(): Int
    {
        return uuid.hashCode()
    }
}

data class DiscordUser(val id: Snowflake)