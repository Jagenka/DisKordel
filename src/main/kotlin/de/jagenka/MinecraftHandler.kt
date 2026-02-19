package de.jagenka

import de.jagenka.config.Config
import dev.kord.core.entity.Message
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.PlayerChatMessage
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import org.slf4j.LoggerFactory
import kotlin.math.min

object MinecraftHandler
{
    val logger = LoggerFactory.getLogger("diskordel")

    var minecraftServer: MinecraftServer? = null

    // to set MinecraftServer instance coming from Mixin (actually from FabricAPI)
    fun onServerLoaded(minecraftServer: MinecraftServer)
    {
        this.minecraftServer = minecraftServer

        minecraftServer.isUsingWhitelist = true

        Main.scope.launch {
            // make sure Diskordel user cache is filled with available data as much as possible
            UserRegistry.loadDiskordelUserCache()
            // UserRegistry.loadGameProfilesToCacheFromMinecraftServicesWithAvailableFiles() gone, as it may be unnecessary

            // load registered users with available cache data
            UserRegistry.loadRegisteredUsersFromDiskordelConfig()
        }
    }

    fun registerMixins()
    {
        // register chat message
        ServerMessageEvents.CHAT_MESSAGE.register { message, sender, _ ->
            Main.scope.launch {
                handleMinecraftChatMessage(message.decoratedContent(), sender)
            }
        }

        // say/me messages
        ServerMessageEvents.COMMAND_MESSAGE.register { message, _, parameters ->
            handleSayCommand(message, parameters)
        }

        // login messages
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val player = handler.player
            Main.scope.launch {
                val text = Component.translatable("multiplayer.player.joined", player.displayName)
                val string = text.string
                val name = string.split(" ").firstOrNull()
                sendSystemMessageAsPlayer(name, string)
            }
        }

        // disconnect messages
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            val player = handler.player
            Main.scope.launch {
                val text = Component.translatable("multiplayer.player.left", player.displayName)
                val string = text.string
                val name = string.split(" ").firstOrNull()
                sendSystemMessageAsPlayer(name, string)
            }
        }
    }

    // coming from AdvancementFrameMixin
    @JvmStatic
    fun handleAdvancementGet(text: Component)
    {
        Main.scope.launch {
            val string = text.string
            val name = string.split(" ").firstOrNull()
            sendSystemMessageAsPlayer(name, string)
        }
    }

    // coming from PlayerDyingMixin
    @JvmStatic
    fun handleDeathMessage(string: String)
    {
        Main.scope.launch {
            val name = string.split(" ").firstOrNull()
            sendSystemMessageAsPlayer(name, string)
        }
    }

    private suspend fun handleMinecraftChatMessage(message: Component, sender: ServerPlayer)
    {
        val user = UserRegistry.getMinecraftUser(sender.uuid) ?: return
        DiscordHandler.sendWebhookMessage(username = user.username, avatarURL = user.getSkinURL(), text = message.string)
    }

    private fun handleSayCommand(message: PlayerChatMessage, params: ChatType.Bound)
    {
        Main.scope.launch {
            val user = UserRegistry.getMinecraftUser(message.sender())
            val text = message.decoratedContent()

            DiscordHandler.sendWebhookMessage(
                username = Config.configEntry.discordSettings.serverName,
                text = if (user != null) "[${user.username}] ${text.string}" else text.string,
                escapeMarkdown = false
            )
        }
    }

    /**
     * Sends a message looking like it came from a player, but stylized with > and cursive text.
     * If playerName is not a known player name, the message is sent as whatever is set as `serverName` in Config
     * If playerName is known, it removes content's first word if it is the player's name.
     */
    private suspend fun sendSystemMessageAsPlayer(playerName: String?, content: String)
    {
        val user = playerName?.let { UserRegistry.getMinecraftUser(it) }

        DiscordHandler.sendWebhookMessage(
            username = user?.username ?: Config.configEntry.discordSettings.serverName,
            avatarURL = user?.getSkinURL() ?: "",
            text = "> *${
                if (playerName != null && content.startsWith(playerName, ignoreCase = true)) content.replaceFirst(playerName, "", ignoreCase = true).trim() else content
            }*",
            escapeMarkdown = false
        )
    }

    suspend fun sendMessageFromDiscord(message: Message)
    {

    }

    fun getOnlinePlayers(): List<ServerPlayer>
    {
        minecraftServer?.let { server ->
            return server.playerList.players
        }

        return emptyList()
    }

    fun runCommand(cmd: String)
    {
        minecraftServer?.commands?.performPrefixedCommand(minecraftServer?.createCommandSourceStack() ?: return, cmd)
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
            val mspt = server.tickTimesNanos.average() * 1.0E-6 // average is in nanoseconds -> convert to milliseconds
            val possibleTickRate = 1000f / mspt.toFloat()
            val tps = if (server.tickRateManager().isSprinting) possibleTickRate else min(possibleTickRate, server.tickRateManager().tickrate())
            return PerformanceMetrics(mspt, tps)
        }

        return PerformanceMetrics(0.0, 0f)
    }

    fun sendMessageToPlayer(player: ServerPlayer, text: String)
    {
        player.sendSystemMessage(Component.nullToEmpty(text))
    }

    fun sendChatMessage(message: String)
    {
        sendChatMessage(Component.nullToEmpty(message))
    }

    fun sendChatMessage(text: Component)
    {
        minecraftServer?.playerList?.broadcastSystemMessage(text, false)
    }

    fun ServerPlayer.sendPrivateMessage(text: String)
    {
        this.sendSystemMessage(Component.nullToEmpty(text))
    }
}