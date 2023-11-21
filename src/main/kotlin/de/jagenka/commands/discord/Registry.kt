package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.tree.CommandNode
import de.jagenka.DiscordHandler
import de.jagenka.commands.universal.DeathsCommand
import de.jagenka.commands.universal.PlaytimeCommand
import de.jagenka.commands.universal.WhereIsCommand
import de.jagenka.commands.universal.WhoisCommand
import dev.kord.core.Kord
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on

object Registry
{
    private val messageCommandPrefix: String = "!"
    private val interactsWithBots: Boolean = true
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

    private val discordCommands = listOf(
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
    )

    fun setup(kord: Kord)
    {
        registerCommands()

        kord.on<MessageCreateEvent> messageHandling@{
            if (message.getChannelOrNull()?.id != DiscordHandler.channel.id)
            {
                // return if message was not sent in set channel
                return@messageHandling
            }
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
                        text = "Error: ${e.message}\n" +
                                "see `${messageCommandPrefix}help`", silent = true
                    )

                    return@messageHandling
                }
            }

            // if commandHandling fails, it must be a chat message, which is to be relayed to Minecraft
            DiscordHandler.relayChatMessage(messageCreateEvent = this)
        }
    }

    private fun registerCommands()
    {
        discordCommands.forEach { it.registerWithDiscord(commandDispatcher) }
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
}