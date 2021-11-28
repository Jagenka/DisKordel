package de.jagenka

import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.Channel
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.RestClient
import net.minecraft.util.Formatting

object DiscordBot
{
    var initialized = false

    private lateinit var client: DiscordClient
    private lateinit var gateway: GatewayDiscordClient
    private lateinit var restClient: RestClient
    private lateinit var channel: Channel

    fun initialize(token: String, channelId: Long)
    {
        //init DiscordClient
        client = DiscordClient.create(token)
        //init GatewayDiscordClient
        gateway = client.login().block()!!
        //init RestClient
        restClient = gateway.restClient
        //get RestClient AppID
        //var appId = restClient.applicationId.block()!!

        //handle received Messages
        gateway.on(MessageCreateEvent::class.java)
            .filter { event -> !event.message.author.get().isBot }
            .subscribe { event -> processMessage(event.message) }

        channel = gateway.getChannelById(Snowflake.of(channelId)).block()!!

        initialized = true
    }

    @JvmStatic
    fun sendMessage(text: String)
    {
        if (!initialized) return
        if (text.isEmpty() || text.isBlank()) return
        channel.restChannel.createMessage(text).block()
    }

    private fun processMessage(message: Message)
    {
        with(message.content)
        {
            when
            {
                startsWith("!cmd") -> if (isJay(message.author.get())) HackfleischDiskursMod.runCommand(
                    this.removePrefix("!cmd").trim()
                ) //NOT FILTERED!!
                startsWith("!whitelist add") -> HackfleischDiskursMod.runWhitelistAdd(this.removePrefix("!whitelist add").trim())
                equals("!thing") -> HackfleischDiskursMod.doThing()
                else -> HackfleischDiskursMod.broadcastMessage(">${message.author.get().username}< ${message.content}", Formatting.BLUE)
            }
        }
    }

    private fun isJay(user: User): Boolean
    {
        return user.id == Snowflake.of(174579897795084288)
    }
}