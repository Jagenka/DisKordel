package de.jagenka

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.network.MessageType
import net.minecraft.server.MinecraftServer
import net.minecraft.text.LiteralText
import net.minecraft.util.Formatting
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.util.*
import kotlin.collections.ArrayList

@Suppress("UNUSED")
object HackfleischDiskursMod : ModInitializer
{
    private const val MOD_ID = "hackfleisch-diskurs-mod"

    private lateinit var minecraftServer: MinecraftServer

    override fun onInitialize()
    {
        //register commands
        CommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            JayCommand.register(dispatcher)
        }

        val path = FabricLoader.getInstance().configDir.resolve("hackfleisch-diskurs.yaml")
        val confLoader = YamlConfigurationLoader.builder().path(path).build()
        val root = confLoader.load()

        val token = root.node("bot-token").get(String::class.java)
        val guildId = root.node("bot-guild").get(Long::class.java)
        val channelId = root.node("bot-channel").get(Long::class.java)

        if (token == null)
        {
            root.node("bot-token").set("INSERT_TOKEN_HERE")
            confLoader.save(root)
            println("bot-token missing!")
        }
        if (guildId == null)
        {
            root.node("bot-guild").set("INSERT_GUILD_ID_HERE")
            confLoader.save(root)
            println("bot-guild missing!")
        }
        if (channelId == null)
        {
            root.node("bot-channel").set("INSERT_CHANNEL_ID_HERE")
            confLoader.save(root)
            println("bot-channel missing!")
        }
        if (token != null && guildId != null && channelId != null) DiscordBot.initialize(token, guildId, channelId)

        println("hackfleisch-diskurs-mod has been initialized.")
    }

    fun getOnlinePlayers(): List<String>
    {
        if (!checkMinecraftServer()) ArrayList<String>()
        val list = ArrayList<String>()
        minecraftServer.playerManager.playerList.forEach { list.add(it.name.asString()) }
        return list
    }

    @JvmStatic
    fun broadcastMessage(message: String, formatting: Formatting = Formatting.WHITE, sender: UUID = UUID.randomUUID())
    {
        if (!checkMinecraftServer()) return
        val text = LiteralText(message).formatted(formatting)
        minecraftServer.playerManager.broadcast(text, MessageType.CHAT, sender)
    }

    fun doThing()
    {
        if (!checkMinecraftServer()) return
        //minecraftServer.commandManager.execute(minecraftServer.commandSource, "say helö") //cmd coming from MinecraftServer
    }

    fun runCommand(cmd: String)
    {
        if (!checkMinecraftServer()) return
        minecraftServer.commandManager.execute(minecraftServer.commandSource, cmd)
    }

    fun runWhitelistAdd(player: String)
    {
        runCommand("whitelist add $player")
    }

    fun runWhitelistRemove(player: String)
    {
        runCommand("whitelist remove $player")
    }

    private fun checkMinecraftServer(): Boolean
    {
        return HackfleischDiskursMod::minecraftServer.isInitialized
    }

    //to set MinecraftServer instance coming from Mixin
    @JvmStatic
    fun onServerLoaded(minecraftServer: MinecraftServer)
    {
        this.minecraftServer = minecraftServer
    }
}