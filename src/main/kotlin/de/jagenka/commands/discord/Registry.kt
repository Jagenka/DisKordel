package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.tree.CommandNode
import de.jagenka.DiscordHandler
import de.jagenka.Main
import de.jagenka.MinecraftHandler
import de.jagenka.Util
import de.jagenka.Util.unwrap
import de.jagenka.commands.DiskordelCommand
import de.jagenka.commands.DiskordelSlashCommand
import de.jagenka.commands.DiskordelTextCommand
import de.jagenka.commands.universal.*
import de.jagenka.config.Config
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.application.GuildApplicationCommand
import dev.kord.core.entity.effectiveName
import dev.kord.core.entity.toRawType
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageUpdateEvent
import dev.kord.core.on
import kotlinx.coroutines.launch
import net.fabricmc.loader.api.FabricLoader
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI

object Registry
{
    private val linkToAppCommandVersionFile = "https://raw.githubusercontent.com/Jagenka/DisKordel/v${
        FabricLoader.getInstance().getModContainer("diskordel").unwrap()?.metadata?.version?.toString()
    }/applicationCommandsVersion.yaml"

    private const val messageCommandPrefix: String = "!"
    private const val interactsWithBots: Boolean = true
    private val commandDispatcher = CommandDispatcher<MessageCommandSource>()

    /**
     * this stores short help texts. if two texts are equal, it is assumed, that the corresponding commands do the same thing. (important for cleaning up help overview)
     */
    private val shortHelpTexts = mutableMapOf<CommandNode<MessageCommandSource>, String>()

    /**
     * this stores long help texts. if two texts are equal, it is assumed, that the corresponding commands do the same thing.
     * mapping is command literal to help text
     */
    private val longHelpTexts = mutableMapOf<String, String>()

    val commands = listOf<DiskordelCommand>(
        // Discord-only Commands
        PerfCommand,
        ListCommand,
        UsersCommand,
        UpdateNamesCommand,
        RegisterCommand,
        UnregisterCommand,
        StatsCommand,
        RelativeStatsCommand,
        PlaytimePerStatCommand,
        CompareStatsCommand,
        HelpCommand,

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
                val url = URI(linkToAppCommandVersionFile).toURL()
                val conn: HttpURLConnection = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000 // timing out in five seconds
                val version = BufferedReader(InputStreamReader(conn.inputStream)).readLines()
                    .find { it.startsWith("version: ") }?.removePrefix("version: ")

                if (version != Config.configEntry.appCommandVersion)
                {
                    reRegisterApplicationCommands(kord, DiscordHandler.guild.id)
                    Config.configEntry.appCommandVersion = version ?: "0"
                    Config.store()
                }

                return@launch

            } catch (_: Exception)
            {
            }

            if (Config.configEntry.appCommandVersion == "0")
            {
                reRegisterApplicationCommands(kord, DiscordHandler.guild.id)
                return@launch
            }

            val appCommands = mutableListOf<GuildApplicationCommand>()
            kord.getGuildApplicationCommands(DiscordHandler.guild.id).collect { appCommands.add(it) }
            if (commands.filterIsInstance<DiskordelSlashCommand>().size != appCommands.size)
            {
                reRegisterApplicationCommands(kord, DiscordHandler.guild.id)
            }
        }

        registerCommands()

        kord.on<MessageCreateEvent> messageHandling@{
            // return if message is from ourselves
            if (message.author?.id == kord.selfId) return@messageHandling

            // return if message was not sent in set channel
            if (message.getChannelOrNull()?.id != DiscordHandler.channel.id) return@messageHandling

            // return if message is from an interaction, as we catch that in the message update event
            if (message.interaction != null) return@messageHandling

            let commandHandling@{
                if (interactsWithBots)
                {
                    // return if author is self or undefined
                    if (message.author?.id == kord.selfId) return@commandHandling
                } else
                {
                    // return if author is a bot or undefined
                    if (message.author?.isBot != false) return@commandHandling
                }

                if (!message.content.startsWith(messageCommandPrefix)) return@commandHandling

                val command = message.content.removePrefix(messageCommandPrefix)

                try
                {
                    commandDispatcher.execute(command, MessageCommandSource(event = this))
                    return@messageHandling
                } catch (e: CommandSyntaxException)
                {
                    DiscordHandler.sendMessage(
                        text = "DisKordel no longer uses text commands. Use / commands instead!",
                        silent = true,
                    )
//                    DiscordHandler.sendMessage(
//                        text = "Error: ${e.message}\n" +
//                                "see `${messageCommandPrefix}help`", silent = true
//                    )

                    return@messageHandling
                }
            }

            if (message.webhookId == Util.getOrCreateWebhook("diskordel_chat_messages").id) return@messageHandling

            // if commandHandling fails, it must be a chat message, which is to be relayed to Minecraft
            DiscordHandler.relayChatMessage(
                authorId = message.author?.id,
                authorName = message.getAuthorAsMemberOrNull()?.effectiveName ?: message.author?.effectiveName ?: "unknown user",
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

                DiscordHandler.relayChatMessage(
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

    private fun registerCommands()
    {
        // sort commands into types
        commands.forEach { cmd ->
            if (cmd is DiskordelTextCommand)
            {
                // cmd.registerWithDiscord(commandDispatcher)
            }

            if (cmd is DiskordelSlashCommand)
            {
                if (slashCommandMap.containsKey(cmd.name)) error("${cmd.name} is already a registered slash command.")
                slashCommandMap[cmd.name] = cmd
            }
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

    fun registerShortHelpText(text: String, vararg nodes: CommandNode<MessageCommandSource>)
    {
        nodes.forEach { shortHelpTexts[it] = text }
    }

    fun registerLongHelpText(text: String, vararg nodes: CommandNode<MessageCommandSource>)
    {
        nodes.forEach { longHelpTexts[it.name] = text }
    }

    fun getShortHelpTexts(source: MessageCommandSource): List<String>
    {
        val noHelpTexts = mutableListOf<String>()

        val helpTexts = commandDispatcher.getSmartUsage(commandDispatcher.root, source).toList()
            .groupBy { shortHelpTexts[it.first] }
            .mapNotNull {
                val shortHelpText = it.key
                return@mapNotNull if (shortHelpText != null)
                {
                    val smartUsageString = it.value.first().second
                    val trimmedUsageString = if (smartUsageString.contains(' ')) smartUsageString.replaceBefore(' ', "") else ""
                    val postfix = "$trimmedUsageString`: $shortHelpText"
                    it.value.joinToString(separator = "|", postfix = postfix) { it.second.replaceAfter(' ', "").trim() }
                } else
                {
                    noHelpTexts.addAll(it.value.map { it.second + "`" })
                    null
                }
            }.toMutableList()

        helpTexts.addAll(noHelpTexts)

        return helpTexts.sorted().map { "`$messageCommandPrefix$it" }
    }

    fun getHelpTextsForCommand(source: MessageCommandSource, command: String): List<String>
    {
        val parseResults = commandDispatcher.parse(command, source)
        if (parseResults.context.nodes.isEmpty())
        {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create()
        }
        return commandDispatcher.getSmartUsage(parseResults.context.rootNode, source)
            .filter { it.key.name.equals(command, ignoreCase = true) }
            .map { "`!" + it.value + "`" + (longHelpTexts[command]?.let { ": $it" } ?: "") }
    }

    private suspend fun reRegisterApplicationCommands(kord: Kord, guildId: Snowflake)
    {
        MinecraftHandler.logger.info("Start re-registering Discord Application Commands...")

        // delete all commands
        kord.getGuildApplicationCommands(guildId).collect {
            it.delete()
        }

        // register all commands
        commands.filterIsInstance<DiskordelSlashCommand>().forEach { cmd ->
            kord.createGuildChatInputCommand(guildId, cmd.name, cmd.description) {
                cmd.build(this)
            }
        }

        MinecraftHandler.logger.info("Discord Application Commands re-registered!")
    }
}
