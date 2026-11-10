package de.jagenka.commands.discord

import de.jagenka.*
import de.jagenka.commands.DiskordelCommand
import de.jagenka.commands.DiskordelSlashCommand
import de.jagenka.commands.universal.*
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.effectiveName
import dev.kord.core.entity.toRawType
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.core.on
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

object Registry
{
    val commands = listOf<DiskordelCommand>(
        // Discord-only Commands
        PerfCommand,
        ListCommand,
        UsersCommand,
        RegisterCommand,
        UnregisterCommand,
        StatsCommand,
        HelpCommand,
        PingCommand,
        SpeedCommand,

        // Universal Commands
        WhoisCommand,
        WhereIsCommand,
        PlaytimeCommand,
        DeathsCommand,
        EvalCommand,
    )

    private val slashCommandMap = mutableMapOf<String, DiskordelSlashCommand>()

    fun setup(kord: Kord)
    {
        Main.scope.launch {
            try
            {
                ApplicationCommandSync.syncGuildCommands(kord, DiscordHandler.guild.id, commands.filterIsInstance<DiskordelSlashCommand>())
                ApplicationCommandSync.purgeStaleGlobalCommands(kord)
            } catch (e: Exception)
            {
                if (e is CancellationException)
                {
                    ensureActive()
                }
                MinecraftHandler.logger.error("Could not sync Discord Application Commands: ${e.message}")
            }
        }

        indexSlashCommandsByName()

        kord.on<MessageCreateEvent> messageHandling@{
            // return if message is from ourselves
            if (message.author?.id == kord.selfId) return@messageHandling

            // return if message was not sent in set channel
            if (message.getChannelOrNull()?.id != DiscordHandler.channel.id) return@messageHandling

            // return if message is from an interaction, as we catch that in the message update event
            if (message.interaction != null) return@messageHandling

            if (message.webhookId == Util.getOrCreateWebhook("diskordel_chat_messages").id) return@messageHandling

            // every message that reaches here is a chat message, to be relayed to Minecraft
            val member = message.getAuthorAsMemberOrNull()
            if (member != null)
            {
                Main.scope.launch {
                    UserRegistry.updateDiscordName(member)
                }
            }

            DiscordHandler.relayChatMessageToMinecraft(
                authorId = message.author?.id,
                authorName = member?.effectiveName ?: message.author?.effectiveName ?: "unknown user",
                content = messageWithPrettyMentions(message),
                referencedMessage = message.referencedMessage,
                attachments = message.attachments.map { it.toRawType() },
                linkToMessage = Util.getMessageURL(message)
            )
        }

        // here we don't allow commands
        kord.on<MessageUpdateEvent> messageUpdateHandling@{
            // return if message is from ourselves
            if (new.author.value?.id == kord.selfId) return@messageUpdateHandling

            // return if message was not sent in set channel
            if (new.channelId != DiscordHandler.channel.id) return@messageUpdateHandling

            // this is a response from an interaction
            if (new.interaction.value != null)
            {
                val authorName = new.author.value?.globalName?.value ?: new.author.value?.username ?: "unknown user"

                DiscordHandler.relayChatMessageToMinecraft(
                    authorId = new.author.value?.id,
                    authorName = authorName,
                    content = new.content.value ?: "* empty message *",
                    referencedMessage = null,
                    attachments = new.attachments.value ?: emptyList(),
                    linkToMessage = Util.getMessageURL(new.guildId.value, new.channelId, new.id)
                )
            }
        }

        kord.on<ChatInputCommandInteractionCreateEvent> {
            handleSlashCommands(this)
        }
    }

    private suspend fun messageWithPrettyMentions(message: Message): String
    {
        var messageContent = message.content
        message.mentionedUsers.collect {
            messageContent = messageContent.replace(it.mention, "@${it.username}")
        }
        message.mentionedRoles.collect {
            println(it.name)
            messageContent = messageContent.replace(it.mention, "@${it.name}")
        }
        message.mentionedChannels.collect {
            println(it.data.name.value)
            messageContent = messageContent.replace(it.mention, "#${it.data.name.value ?: "someChannel"}")
        }
        return messageContent
    }

    private fun indexSlashCommandsByName()
    {
        commands.filterIsInstance<DiskordelSlashCommand>().forEach { cmd ->
            if (slashCommandMap.containsKey(cmd.name)) error("${cmd.name} is already a registered slash command.")
            slashCommandMap[cmd.name] = cmd
        }
    }

    private suspend fun handleSlashCommands(event: ChatInputCommandInteractionCreateEvent)
    {
        with(event)
        {
            val cmd = slashCommandMap[interaction.invokedCommandName]
            cmd?.execute(this)
        }
    }
}
