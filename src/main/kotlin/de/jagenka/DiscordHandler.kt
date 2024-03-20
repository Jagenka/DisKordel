package de.jagenka

import de.jagenka.commands.discord.Registry
import de.jagenka.commands.discord.UnregisterCommand
import dev.kord.common.entity.DiscordAttachment
import dev.kord.common.entity.MessageFlag
import dev.kord.common.entity.MessageFlags
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.event.guild.MemberUpdateEvent
import dev.kord.core.exception.KordInitializationException
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.addFile
import io.ktor.client.request.forms.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.text.*
import net.minecraft.util.Formatting
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

    private val guildEmojiRegex = Regex("<a?(:[a-zA-Z0-9_]+:)[0-9]+>")

    const val MESSAGE_LENGTH_LIMIT: Int = 2000

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

            Registry.setup(kord)

            kord.login {// nicht sicher ob man für jeden link nen eigenen bot braucht mit der API
                @OptIn(PrivilegedIntent::class)
                intents += Intent.MessageContent
            }

            kord.on<MemberUpdateEvent> {
                if (member.guildId != guildSnowflake) return@on // if name change in different guild, ignore

                if (old?.effectiveName != member.effectiveName)
                {
                    MinecraftHandler.logger.info("updating name for ${member.username}...")
                    UserRegistry.updateDiscordName(member)
                }
            }
        } ?: throw BotInitializationException("Error initializing bot.")
    }

    fun sendMessage(text: String, silent: Boolean = false)
    {
        Main.scope.launch {
            if (text.isBlank()) return@launch

            var toSend = text

            if (text.length > MESSAGE_LENGTH_LIMIT)
            {
                toSend = text.substring(0, MESSAGE_LENGTH_LIMIT - 3) + "..." // trim to 2000 characters as per discord api limit
            }

            channel.createMessage {
                this.content = toSend
                if (silent) this.flags = MessageFlags(MessageFlag.SuppressNotifications)
            }
        }
    }

    fun String.asCodeBlock(formatId: String = ""): String
    {
        if (this.preventCodeBlockEscape().isBlank()) return "*empty*"

        val content = ("$formatId\n" + this.preventCodeBlockEscape())
        var toSend = content

        if (content.length > MESSAGE_LENGTH_LIMIT - 7)
        {
            toSend = content.substring(0, MESSAGE_LENGTH_LIMIT - 10) + "..."
        }

        return "```$toSend\n```"
    }

    fun sendCodeBlock(text: String, formatId: String = "", silent: Boolean = false)
    {
        if (text.preventCodeBlockEscape().isBlank()) return

        val content = ("$formatId\n" + text.preventCodeBlockEscape())
        var toSend = content

        if (content.length > MESSAGE_LENGTH_LIMIT - 7)
        {
            toSend = content.substring(0, MESSAGE_LENGTH_LIMIT - 10) + "..."
        }

        sendMessage(text = "```$toSend\n```", silent = silent)
    }

    suspend fun sendWebhookMessage(username: String, avatarURL: String = Util.getServerIconURL(), text: String, escapeMarkdown: Boolean = true)
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

    suspend fun handleNotAMember(id: Snowflake)
    {
        MinecraftHandler.logger.error("User with Snowflake $id is not a member of the configured guild!")
        UnregisterCommand.unregisterUser(id)
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
    suspend fun relayChatMessage(
        authorId: Snowflake?,
        authorName: String,
        content: String,
        referencedMessage: Message?,
        attachments: Collection<DiscordAttachment>,
        linkToMessage: String
    )
    {
        val associatedUser = UserRegistry.findUser(authorId)

        val authorText = Text.of(
            "[$authorName]"
        ).getWithStyle(
            Style.EMPTY
                .withFormatting(Formatting.BLUE)
                .withHoverEvent(associatedUser?.minecraft?.name?.let { HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.of(it)) })
        ).firstOrNull()

        val referencedAuthorText = Text.of(
            referencedMessage?.getAuthorAsMemberOrNull()?.let {
                "@${it.effectiveName}"
            } ?: referencedMessage?.data?.author?.username?.let {
                "@$it"
            } ?: ""
        ).getWithStyle(
            Style.EMPTY
                .withFormatting(Formatting.BLUE)
                .withHoverEvent(
                    HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        Text.of(referencedMessage?.author?.let { UserRegistry.findUser(it.id)?.minecraft?.name })
                    )
                )
                .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, Util.getMessageURL(referencedMessage)))
        ).firstOrNull()

        val messageContent =
            if (attachments.isEmpty())
            {
                // simplify guild emojis
                content.replace(guildEmojiRegex) { matchResult ->
                    matchResult.groups[1]?.value ?: matchResult.value // index is 1, as groups are 1-indexed
                }
            } else
            {
                "* view attachment in Discord *"
            }

        val messageText = Text.of(messageContent)
            .getWithStyle(
                Style.EMPTY
                    .withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, linkToMessage))
            )
            .firstOrNull()

        MinecraftHandler.sendChatMessage(Texts.join(listOfNotNull(authorText, referencedAuthorText, messageText), Text.of(" ")))
    }
}