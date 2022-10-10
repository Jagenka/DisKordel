package de.jagenka

import de.jagenka.config.Config
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import kotlinx.coroutines.launch

class LinkedChannel(val channel: MessageChannelBehavior, val guild: GuildBehavior, val users: Users = Users())
{
    init
    {
        Main.scope.launch {
            loadUsersFromFile()
        }
    }

    //TODO: load every so often
    suspend fun loadUsersFromFile()
    {
        users.clear()

        Config.configEntry.users.forEach { (discordId, minecraftName) ->
            val memberSnowflake = Snowflake(discordId)
            val member = guild.getMemberOrNull(memberSnowflake)
            if (member != null) users.put(member, minecraftName)
            else handleNotAMember(memberSnowflake)
        }
    }

    private suspend fun handleNotAMember(id: Snowflake)
    {
        sendMessage("ERROR: user with id ${id.value} is not a member of configured guild!")
    }

    suspend fun sendMessage(text: String)
    {
        if (text.isBlank()) return
        channel.createMessage(text)
    }
}