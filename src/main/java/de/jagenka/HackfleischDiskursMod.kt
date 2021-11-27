package de.jagenka

import discord4j.core.DiscordClient
import discord4j.core.`object`.entity.Message
import discord4j.core.event.domain.message.MessageCreateEvent
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.MessageType
import net.minecraft.server.MinecraftServer
import net.minecraft.text.LiteralText
import net.minecraft.util.Formatting
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.util.*

@Suppress("UNUSED")
object HackfleischDiskursMod : ModInitializer
{
    private const val MOD_ID = "hackfleisch-diskurs-mod"

    //TODO catch usage before init
    private lateinit var minecraftServer: MinecraftServer

    override fun onInitialize()
    {
        //register commands
        CommandRegistrationCallback.EVENT.register { dispatcher, dedicated ->
            JayCommand.register(dispatcher)
        }

        val path = FabricLoader.getInstance().configDir.resolve("hackfleisch-diskurs.yaml")
        val confLoader = YamlConfigurationLoader.builder().path(path).build()
        val root = confLoader.load()
        val token = root.node("bot-token").get(String::class.java)

        if (token != null) startDiscordBot(token)
        else
        { //TODO: centralize config creation
            root.node("bot-token").set("INSERT_TOKEN_HERE")
            confLoader.save(root)
            println("Bot Token missing!")
        }

        println("hackfleisch-diskurs-mod has been initialized.")
    }

    @JvmStatic
    fun broadcastMessage(message: String, formatting: Formatting, sender: UUID)
    {
        val text = LiteralText(message).formatted(formatting)
        minecraftServer.playerManager.broadcastChatMessage(text, MessageType.CHAT, sender)
    }

    private fun startDiscordBot(token: String)
    {
        //init DiscordClient
        val client = DiscordClient.create(token)
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
        broadcastMessage(
            "[Discord] ${message.author.get().username} said \"${message.content}\"",
            Formatting.WHITE,
            UUID.randomUUID()
        )
    }

    //to set MinecraftServer instance coming from Mixin
    @JvmStatic
    fun onServerLoaded(minecraftServer: MinecraftServer)
    {
        this.minecraftServer = minecraftServer
    }
}