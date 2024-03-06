package de.jagenka

import de.jagenka.Util.unwrap
import de.jagenka.config.Config
import dev.kord.core.entity.effectiveName
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.launch
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.advancement.AdvancementEntry
import net.minecraft.network.message.MessageType
import net.minecraft.network.message.SignedMessage
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.*
import net.minecraft.util.Formatting
import org.slf4j.LoggerFactory
import kotlin.math.min

object MinecraftHandler
{
    private val guildEmojiRegex = Regex("<a?(:[a-zA-Z0-9_]+:)[0-9]+>")

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
            val text = entity.damageTracker.deathMessage
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
    fun handleAdvancementGet(advancement: AdvancementEntry, player: ServerPlayerEntity)
    {
        val display = advancement.value.display.unwrap() ?: return
        if (!display.shouldAnnounceToChat()) return
        val text = display.frame.getChatAnnouncementText(advancement, player)
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

    suspend fun sendMessageFromDiscord(event: MessageCreateEvent)
    {
        val author = event.message.author
        val authorName = event.message.getAuthorAsMemberOrNull()?.effectiveName ?: author?.effectiveName ?: "unknown user"
        val associatedUser = UserRegistry.findUser(author?.id)

        val authorText = Text.of(
            "[$authorName]"
        ).getWithStyle(
            Style.EMPTY
                .withFormatting(Formatting.BLUE)
                .withHoverEvent(associatedUser?.minecraft?.name?.let { HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(it)) })
        ).firstOrNull()

        val referencedAuthorText = Text.of(
            event.message.referencedMessage?.getAuthorAsMemberOrNull()?.let {
                "@${it.effectiveName}"
            } ?: event.message.referencedMessage?.data?.author?.username?.let {
                "@$it"
            } ?: ""
        ).getWithStyle(
            Style.EMPTY
                .withFormatting(Formatting.BLUE)
                .withHoverEvent(
                    HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Text.of(event.message.referencedMessage?.author?.let { UserRegistry.findUser(it.id)?.minecraft?.name })
                    )
                )
                .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, Util.getMessageURL(event.message.referencedMessage ?: event.message)))
        ).firstOrNull()

        val messageContent =
            if (event.message.attachments.isEmpty())
            {
                // simplify guild emojis
                event.message.content.replace(guildEmojiRegex) { matchResult ->
                    matchResult.groups[1]?.value ?: matchResult.value // index is 1, as groups are 1-indexed
                }
            } else
            {
                "* view attachment in Discord *"
            }

        val messageText = Text.of(messageContent)
            .getWithStyle(
                Style.EMPTY
                    .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, Util.getMessageURL(event.message)))
            )
            .firstOrNull()

        sendChatMessage(Texts.join(listOfNotNull(authorText, referencedAuthorText, messageText), Text.of(" ")))
    }

    fun getOnlinePlayers(): List<String>
    {
        minecraftServer?.let { server ->
            return server.playerManager.playerList.map { it.name.string }
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