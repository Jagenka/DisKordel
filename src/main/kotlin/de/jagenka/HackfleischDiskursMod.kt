package de.jagenka

import discord4j.core.DiscordClient
import discord4j.core.`object`.entity.Message
import discord4j.core.event.domain.message.MessageCreateEvent
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.minecraft.network.MessageType
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.dedicated.MinecraftDedicatedServer
import net.minecraft.text.LiteralText
import net.minecraft.util.Formatting
import java.util.*
import kotlin.math.min

@Suppress("UNUSED")
object HackfleischDiskursMod : ModInitializer
{
    //TODO: hide!
    private const val TOKEN = ""
    private const val MOD_ID = "hackfleisch-diskurs-mod"

    private lateinit var minecraftServer: MinecraftServer

    override fun onInitialize()
    {
        //register commands
        CommandRegistrationCallback.EVENT.register { dispatcher, dedicated ->
            JayCommand.register(dispatcher)
        }

        //startDiscordBot()

        println("hackfleisch-diskurs-mod has been initialized.")
    }

    @JvmStatic
    fun onServerLoaded(minecraftServer: MinecraftServer)
    {
        this.minecraftServer = minecraftServer
    }

    @JvmStatic
    fun broadcastMessage(source: ServerCommandSource, formatting: Formatting, message: String, sender: UUID)
    {
        val text = LiteralText(message).formatted(formatting)
        minecraftServer.playerManager.broadcastChatMessage(text, MessageType.CHAT, sender)
    }

    private fun startDiscordBot()
    {
        //init DiscordClient
        val client = DiscordClient.create(TOKEN)
        //init GatewayDiscordClient
        val gateway = client.login().block()!!
        //init RestClient
        val restClient = gateway.restClient
        //get RestClient AppID
        val appId = restClient.applicationId.block()!!

        //handle received Messages
        gateway.on(MessageCreateEvent::class.java)
            .filter { event -> !event.message.author.get().isBot }
            .subscribe { event -> processDiscordMessage(event.message) }
    }

    private fun processDiscordMessage(message: Message)
    {
        println("A Message from Discord: ${message.author.get().username} said \"${message.content}\"")
    }
}