package de.jagenka

import de.jagenka.commands.discord.*
import de.jagenka.commands.discord.structure.Registry
import dev.kord.common.entity.MessageFlag
import dev.kord.common.entity.MessageFlags
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.exception.KordInitializationException
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.create.addFile
import dev.kord.x.emoji.Emojis
import io.ktor.client.request.forms.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Path
import javax.imageio.ImageIO

object DiscordHandler
{
    var kord: Kord? = null
        private set

    lateinit var guild: GuildBehavior
        private set
    lateinit var channel: MessageChannelBehavior
        private set

    suspend fun init(token: String, guildSnowflake: Snowflake, channelSnowflake: Snowflake)
    {
        try
        {
            kord = Kord(token)
        } catch (e: KordInitializationException)
        {
            throw BotInitializationException(e)
        }

        kord?.let { kord ->
            guild = GuildBehavior(guildSnowflake, kord)
            channel = MessageChannelBehavior(channelSnowflake, kord)

            registerCommands()

            Registry.setup(kord)

            kord.login {// nicht sicher ob man für jeden link nen eigenen bot braucht mit der API
                @OptIn(PrivilegedIntent::class)
                intents += Intent.MessageContent
            }
        } ?: throw BotInitializationException("Error initializing bot.")
    }

    private fun registerCommands()
    {
        with(Registry)
        {
            register(HelpMessageCommand)
            register(ListCommand)
            register(RegisterCommand)
            register(UsersCommand)
            register(UpdateNamesCommand)
            register(PerfCommand)
            register(UnregisterCommand)
            register(StatsCommand)
            register(RelativeStatsCommand)
        }
    }

    fun sendMessage(text: String, silent: Boolean = false)
    {
        Main.scope.launch {
            if (text.isBlank()) return@launch

            var toSend = text

            if (text.length > 2000)
            {
                toSend = text.substring(0, 1997) + "..." // trim to 2000 characters as per discord api limit
            }

            channel.createMessage {
                this.content = toSend
                if (silent) this.flags = MessageFlags(MessageFlag.SuppressNotifications)
            }
        }
    }

    fun sendCodeBlock(formatId: String = "", text: String)
    {
        val content = ("$formatId\n" + text.preventCodeBlockEscape())
        var toSend = content

        if (content.length > 1993)
        {
            toSend = content.substring(0, 1990) + "..."
        }

        sendMessage("```$toSend\n```")
    }

    suspend fun sendWebhookMessage(username: String, avatarURL: String, text: String, escapeMarkdown: Boolean = true)
    {
        if (text.isBlank()) return

        val webhook = Util.getOrCreateWebhook("diskordel_chat_messages")
        kord?.apply {
            rest.webhook.executeWebhook(webhookId = webhook.id, token = webhook.token.value ?: "") {
                this.username = username
                this.avatarUrl = avatarURL
                this.content =
                    text.let { if (it.length > 2000) it.substring(0, 1997) + "..." else it } // trim as per API limit
                        .let { if (escapeMarkdown) it.markdownEscaped() else it }
            }
        }
    }

    suspend fun sendImage(path: Path, silent: Boolean = false): Message
    {
        return channel.createMessage {
            this.addFile(path)
            if (silent) flags = MessageFlags(MessageFlag.SuppressNotifications)
        }
    }

    suspend fun sendImage(fileName: String, inputStream: InputStream, silent: Boolean = false): Message
    {
        return channel.createMessage {
            this.addFile(fileName, ChannelProvider { inputStream.toByteReadChannel() })
            if (silent) flags = MessageFlags(MessageFlag.SuppressNotifications)
        }
    }

    suspend fun sendImage(fileName: String, bufferedImage: BufferedImage, silent: Boolean = false): Message
    {
        val outputStream = ByteArrayOutputStream()
        withContext(Dispatchers.IO) {
            ImageIO.write(bufferedImage, "png", outputStream)
        }
        val inputStream = ByteArrayInputStream(outputStream.toByteArray())
        return sendImage(fileName, inputStream, silent)
    }

    private fun String.preventCodeBlockEscape(): String
    {
        return this
            .replace("```", "\\`\\`\\`")
    }

    suspend fun getMemberOrSendError(id: Snowflake): Member?
    {
        guild.getMemberOrNull(id)?.let {
            return it
        }

        handleNotAMember(id)
        return null
    }

    fun handleNotAMember(id: Snowflake)
    {
        MinecraftHandler.logger.error("User with Snowflake $id is not a member of the configured guild!")
    }

    fun reactConfirmation(message: Message)
    {
        Main.scope.launch {
            message.addReaction(ReactionEmoji.Unicode(Emojis.whiteCheckMark.unicode))
        }
    }

    /**
     * prevents messages from looking like a system message, as implemented [MinecraftHandler.sendSystemMessageAsPlayer]
     */
    private fun String.markdownEscaped(): String
    {
        return this.replace("""> """, """\> """)
    }

    fun Member.prettyName() = "${this.effectiveName} (${this.username})"

    /**
     * this is called, if a message is not a command, so if it is a chat message
     */
    suspend fun handleNotACommand(event: MessageCreateEvent)
    {
        if (event.message.author?.id == kord?.selfId || event.message.webhookId == Util.getOrCreateWebhook("diskordel_chat_messages").id) return

        MinecraftHandler.sendMessageFromDiscord(event)
    }
}