package de.jagenka

import de.jagenka.config.Config
import dev.kord.core.entity.Message
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.network.message.MessageType
import net.minecraft.network.message.SignedMessage
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
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

        minecraftServer.playerManager.isWhitelistEnabled = true

        Main.scope.launch {
            UserRegistry.loadUserCache()
            UserRegistry.loadRegisteredUsersFromFile()
            UserRegistry.loadGameProfilesFromPlayerData()
            UserRegistry.saveCacheToFile()
            UserRegistry.prepareNamesForComparison()
        }
    }

    fun registerMixins()
    {
        // register chat message
        ServerMessageEvents.CHAT_MESSAGE.register { message, sender, _ ->
            Main.scope.launch {
                handleMinecraftChatMessage(message.content, sender)
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
                val text = Text.translatable("multiplayer.player.joined", player.displayName)
                val string = text.string
                val name = string.split(" ").firstOrNull()
                sendSystemMessageAsPlayer(name, string)
            }
        }

        // disconnect messages
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            val player = handler.player
            Main.scope.launch {
                val text = Text.translatable("multiplayer.player.left", player.displayName)
                val string = text.string
                val name = string.split(" ").firstOrNull()
                sendSystemMessageAsPlayer(name, string)
            }
        }

        // death messages
        ServerLivingEntityEvents.ALLOW_DEATH.register { entity, _, _ ->
            if (!entity.isPlayer) return@register true
            val text = entity.damageTracker.deathMessage.copy()
            Main.scope.launch {
                val string = text.string
                val name = string.split(" ").firstOrNull()
                sendSystemMessageAsPlayer(name, string)
            }

            return@register true
        }
    }

    // coming from Mixin, as I did not find an inject from Fabric API
    @JvmStatic
    fun handleAdvancementGet(text: Text)
    {
        Main.scope.launch {
            val string = text.string
            val name = string.split(" ").firstOrNull()
            sendSystemMessageAsPlayer(name, string)
        }
    }

    private suspend fun handleMinecraftChatMessage(message: Text, sender: ServerPlayerEntity)
    {
        val user = UserRegistry.getMinecraftUser(sender.uuid) ?: return
        DiscordHandler.sendWebhookMessage(username = user.name, avatarURL = user.getSkinURL(), text = message.string)
    }

    private fun handleSayCommand(message: SignedMessage, params: MessageType.Parameters)
    {
        Main.scope.launch {
            val user = UserRegistry.getMinecraftUser(message.sender)
            val text = message.content

            DiscordHandler.sendWebhookMessage(
                username = Config.configEntry.discordSettings.serverName,
                text = if (user != null) "[${user.name}] ${text.string}" else text.string,
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
            username = user?.name ?: Config.configEntry.discordSettings.serverName,
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

    fun getOnlinePlayers(): List<ServerPlayerEntity>
    {
        minecraftServer?.let { server ->
            return server.playerManager.playerList
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
            val mspt = server.tickTimes.average() * 1.0E-6 // average is in nanoseconds -> convert to milliseconds
            val possibleTickRate = 1000f / mspt.toFloat()
            val tps = if (server.tickManager.isSprinting) possibleTickRate else min(possibleTickRate, server.tickManager.tickRate)
            return PerformanceMetrics(mspt, tps)
        }

        return PerformanceMetrics(0.0, 0f)
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