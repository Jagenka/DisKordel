package de.jagenka

import de.jagenka.config.Config
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior

class LinkedChannel(val channel: MessageChannelBehavior, val guild: GuildBehavior, val users: Users = Users())
{
    init
    {
        loadUsersFromFile()
    }

    //TODO: load every so often
    fun loadUsersFromFile()
    {
        users.clear()

        Config.configEntry.users.forEach { (discordId, minecraftName) ->
            val memberSnowflake = Snowflake(discordId)
            val member = ChannelHandler.guild?.getMember(memberSnowflake)
            if (member != null) users.put(member, minecraftName)
            else ChannelHandler.handleNotAMember(memberSnowflake)
        }
    }
}