package de.jagenka

import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.Channel
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.RestClient
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Formatting
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.util.regex.Pattern

object DiscordBot
{
    var initialized = false

    private val usersFilePath = FabricLoader.getInstance().configDir.resolve("hackfleisch-diskurs-users.yaml")

    private lateinit var token: String
    private lateinit var guildId: Snowflake
    private lateinit var channel: Channel

    private lateinit var client: DiscordClient
    private lateinit var gateway: GatewayDiscordClient
    private lateinit var restClient: RestClient

    private val users = Users()

    fun initialize(token: String, guildId: Long, channelId: Long)
    {
        this.token = token

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

        this.guildId = Snowflake.of(guildId)
        channel = gateway.getChannelById(Snowflake.of(channelId)).block()!!

        loadUsersFromFile()

        initialized = true
    }

    @JvmStatic
    fun sendMessageFromMinecraft(sender: String, text: String)
    {
//        val senderSnowflake = users.getKeyForValue(sender)
//        println(senderSnowflake)
//        if (senderSnowflake != null)
//        {
//            val member = gateway.getMemberById(guildId, senderSnowflake).block()
//            println(member)
//            if (member != null)
//            {
//                sendMessage("<${member.displayName}> $text")
//                return
//            }
//        }

        sendMessage("<$sender> ${text.convertMentions()}")
    }

    private fun sendMessage(text: String)
    {
        if (!initialized) return
        if (text.isEmpty() || text.isBlank()) return
        channel.restChannel.createMessage(text).block()
    }

    private fun registerUser(snowflake: Snowflake, minecraftName: String)
    {
        users.put(snowflake, minecraftName)
        saveUsersToFile()
    }

    private fun loadUsersFromFile()
    {
        val confLoader = YamlConfigurationLoader.builder().path(usersFilePath).build()
        val root = confLoader.load()
        repeat(root.childrenList().size) {
            val discordId = root.node(it, "discordId").getLong(0)
            val minecraftName = root.node(it, "minecraftName").getString("")
            users.put(Snowflake.of(discordId), minecraftName)
        }
    }

    private fun saveUsersToFile()
    {
        val confLoader = YamlConfigurationLoader.builder().path(usersFilePath).build()
        val root = confLoader.load()
        users.getConfigList().forEachIndexed { index, entry ->
            root.node(index, "discordId").set(entry.discordId)
            root.node(index, "minecraftName").set(entry.minecraftName)
        }
        confLoader.save(root)
    }

    private fun sendMinecraftMessage(sender: String, text: String)
    {
        HackfleischDiskursMod.broadcastMessage(">$sender< $text", Formatting.BLUE)
    }

    private fun String.convertMentions(): String
    {
        var newString = this
        val matcher = Pattern.compile("@.*@").matcher(this)
        while (matcher.find())
        {
            val mention = matcher.group().substring(1, length-1)
            val memberId = users.getDiscordMember(mention, gateway, guildId)
            if (memberId != null) newString = newString.replaceRange(matcher.start(), matcher.end(), "<@!${memberId.asLong()}>")
        }

        return newString
    }

    private fun printOnlinePlayers()
    {
        val onlinePlayers = HackfleischDiskursMod.getOnlinePlayers()
        val sb = StringBuilder("Currently online: ")
        if (onlinePlayers.isEmpty()) sb.append("~nobody~, ")
        else onlinePlayers.forEach { sb.append("$it, ") }
        sb.deleteRange(sb.length - 2, sb.length)
        sendMessage(sb.toString())
    }

    private fun processMessage(message: Message)
    {
        with(message.content)
        {
            when
            {
                equals("!list") ->
                {
                    printOnlinePlayers()
                }
                startsWith("!register") ->
                {
                    registerUser(message.author.get().id, this.removePrefix("!register").trim())
                }
                equals("!users") ->
                {
                    for (pair in users.getAsSet())
                    {
                        sendMessage(pair.toString())
                    }
                }
                startsWith("!cmd") ->
                {
                    if (isJay(message.author.get()))
                    {
                        HackfleischDiskursMod.runCommand(this.removePrefix("!cmd").trim())
                    } else
                    {
                        null
                    }
                } //NOT FILTERED!!
                startsWith("!whitelist add") ->
                {
                    HackfleischDiskursMod.runWhitelistAdd(this.removePrefix("!whitelist add").trim())
                }
                equals("!thing") -> HackfleischDiskursMod.doThing()
                else ->
                {
                    val member = gateway.getMemberById(guildId, message.author.get().id).block()
                    sendMinecraftMessage(if (member != null) member.displayName else message.author.get().username, message.content)
                }
            }
        }
    }

    private fun isJay(user: User): Boolean
    {
        return user.id == Snowflake.of(174579897795084288)
    }
}

@ConfigSerializable
data class UsersConfigEntry(val discordId: Long, val minecraftName: String)
