package de.jagenka

import de.jagenka.Util.unwrap
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.Position
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.min

object MinecraftHandler
{
    val logger = LoggerFactory.getLogger("diskordel")

    var minecraftServer: MinecraftServer? = null

    // to set MinecraftServer instance coming from Mixin (actually from FabricAPI)
    fun onServerLoaded(minecraftServer: MinecraftServer)
    {
        this.minecraftServer = minecraftServer

        minecraftServer.playerManager.isWhitelistEnabled = true

        DiscordHandler.loadUsersFromFile()
        UserRegistry.loadGameProfilesFromPlayerData()
        UserRegistry.loadUserCache()
    }

    fun registerMixins()
    {
        //register chat message
        ServerMessageEvents.CHAT_MESSAGE.register { message, sender, params ->
            Main.scope.launch {
                handleMinecraftChatMessage(message.content, sender)
            }
        }

        //register system message
        ServerMessageEvents.GAME_MESSAGE.register { server, message, overlay ->
            Main.scope.launch {
                handleMinecraftSystemMessage(message)
            }
        }
    }

    private fun handleMinecraftChatMessage(message: Text, sender: ServerPlayerEntity)
    {
        Main.scope.launch {
            DiscordHandler.sendCodeBlock(
                "ansi", "<\u001B[1;2m${sender.name.string}\u001B[0m> ${message.string}"
            )
        }
    }

    private fun handleMinecraftSystemMessage(message: Text)
    {
        Main.scope.launch {
            if (message.string.startsWith(">") || message.string.startsWith("@")) return@launch // this is a message coming from discord

            val colorName = message.visit({ style, string ->
                val color = style.color ?: return@visit Optional.empty()
                if (color.name != color.hexCode) return@visit Optional.of(color.name)
                Optional.empty()
            }, Style.EMPTY).unwrap()

            DiscordHandler.sendCodeBlock(
                "ansi",
                when (colorName)
                {
                    "yellow" -> // yellow text
                    {
                        "\u001B[2;33m${message.string}\u001B[0m"
                    }

                    "green" -> // green text
                    {
                        "\u001B[2;32m${message.string}\u001B[0m"
                    }

                    "dark_purple" -> // purple text
                    {
                        "\u001B[2;35m${message.string}\u001B[0m"
                    }

                    else -> // red text
                    {
                        "\u001B[2;31m${message.string}\u001B[0m"
                    }
                }
            )
        }
    }

    suspend fun sendMessage(event: MessageCreateEvent)
    {

        var text = event.message.referencedMessage?.getAuthorAsMemberOrNull()?.let {
            "@${it.effectiveName} "
        } ?: ""
        text += ">${event.message.getAuthorAsMemberOrNull()?.effectiveName ?: "noname"}< ${event.message.content}"

        sendChatMessage(Text.literal(text).getWithStyle(Style.EMPTY.withFormatting(Formatting.BLUE))[0])
    }

    fun getOnlinePlayers(): List<String>
    {
        minecraftServer?.let { server ->
            val list = ArrayList<String>()
            server.playerManager.playerList.forEach { list.add(it.name.string) }
            return list
        }

        return emptyList()
    }

    fun runCommand(cmd: String)
    {
        minecraftServer?.commandManager?.executeWithPrefix(minecraftServer?.commandSource, cmd)
    }

    fun runWhitelistAdd(player: String)
    {
        if (player.isEmpty()) return
        runCommand("whitelist add $player")
    }

    fun runWhitelistRemove(player: String)
    {
        if (player.isEmpty()) return
        runCommand("whitelist remove $player")
        runCommand("kick $player")
    }

    fun getPerformanceMetrics(): PerformanceMetrics
    {
        minecraftServer?.let { server ->
            val mspt = server.lastTickLengths.average() * 1.0E-6 // average is in nanoseconds -> convert to milliseconds
            val tps = min(1000.0 / mspt, 20.0)

            return PerformanceMetrics(mspt, tps)
        }

        return PerformanceMetrics(0.0, 0.0)
    }

    fun getPlayerPosition(playerString: String): Position?
    {
        minecraftServer?.let { server ->
            val player = server.playerManager.getPlayer(playerString) ?: return null
            return player.pos
        }
        return null
    }

    fun sendMessageToPlayer(player: ServerPlayerEntity, text: String)
    {
        player.sendMessage(Text.of(text))
    }

    fun sendChatMessage(message: String)
    {
        sendChatMessage(Text.of(message))
    }

    fun sendChatMessage(text: Text)
    {
        minecraftServer?.playerManager?.broadcast(text, false)
    }

    fun ServerPlayerEntity.sendPrivateMessage(text: String)
    {
        this.sendMessage(Text.of(text))
    }
}