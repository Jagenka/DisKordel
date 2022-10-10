package de.jagenka

import de.jagenka.DiscordHandler.markdownSafe
import kotlinx.coroutines.launch
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Position
import kotlin.math.min

object MinecraftHandler
{
    private var minecraftServer: MinecraftServer? = null

    // to set MinecraftServer instance coming from Mixin
    @JvmStatic
    fun onServerLoaded(minecraftServer: MinecraftServer)
    {
        this.minecraftServer = minecraftServer

        minecraftServer.playerManager.isWhitelistEnabled = true
    }

    @JvmStatic
    fun handleMinecraftChatMessage(message: Text, sender: ServerPlayerEntity?)
    {
        Main.scope.launch { DiscordHandler.sendMessage("<${sender?.name?.string}> ${message.string.markdownSafe()}") }
    }

    @JvmStatic
    fun handleMinecraftSystemMessage(message: Text)
    {
        Main.scope.launch {
            if (message.string.startsWith(">")) return@launch
            DiscordHandler.sendMessage(message.string.markdownSafe())
        }
    }

    fun sendMessage(sender: String, text: String)
    {
        sendChatMessage(Text.literal(">$sender< $text").getWithStyle(Style.EMPTY.withFormatting(Formatting.BLUE))[0])
    }

    fun getScoreFromScoreboard() //TODO
    {
        minecraftServer?.let { server ->
            server.scoreboard.getAllPlayerScores(server.scoreboard.getObjective("deaths")).forEach { println("${it.playerName}: ${it.score}") }
        }
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

    fun doThing()
    {

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
    }

    fun getPerformanceMetrics(): PerformanceMetrics
    {
        minecraftServer?.let { server ->
            val mspt = MathHelper.average(server.lastTickLengths) * 1.0E-6
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