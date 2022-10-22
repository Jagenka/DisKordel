package de.jagenka

import de.jagenka.Util.trim
import de.jagenka.commands.DeathsCommand
import de.jagenka.commands.DiscordCommandRegistry
import de.jagenka.commands.PlaytimeCommand
import de.jagenka.config.Config
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.entity.Member
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import kotlinx.coroutines.launch
import java.util.regex.Pattern

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
        kord = Kord(token)

        kord?.let { kord ->
            guild = GuildBehavior(guildSnowflake, kord)
            channel = MessageChannelBehavior(channelSnowflake, kord)

            loadUsersFromFile()

            kord.on<MessageCreateEvent> {
                // return if author is a bot or undefined
                if (message.author?.isBot != false) return@on
                if (message.channelId != channelSnowflake) return@on
                DiscordCommandRegistry.handleCommand(this) //TODO: send to Minecraft if not a command
            }

            kord.login {// nicht sicher ob man für jeden link nen eigenen bot braucht mit der API
                @OptIn(PrivilegedIntent::class)
                intents += Intent.MessageContent
            }
        } ?: error("error initializing bot")
    }

    private suspend fun handleMessage(event: MessageCreateEvent) // TODO: alles auslagern
    {
        // TODO: handle discord messages here
        with(event.message.content)
        {
            when
            {
                equals("!help") ->
                {
                    sendHelpText()
                }

                equals("!list") ->
                {
                    printOnlinePlayers()
                }

                startsWith("!register") ->
                {
                    event.message.author?.let {
                        registerUser(it.id, this.removePrefix("!register").trim())
                    }
                }

                equals("!users") ->
                {
                    printRegisteredUsers()
                }

                startsWith("!whois") ->
                {
                    val input = this.removePrefix("!whois").trim()
                    sendMessage(Users.whoIsPrintable(input))
                }

                equals("!updatenames") ->
                {
                    loadUsersFromFile()
                    //TODO: add reaction
                }

                startsWith("!whitelist") ->
                {
                    event.message.author?.let { ensureWhitelist(it.id) }
                }

                equals("!perf") ->
                {
                    printPerfMetrics()
                }

                startsWith("!deaths") ->
                {
                    val input = this.removePrefix("!deaths").trim()
                    val results = DeathsCommand.getDeathLeaderboardStrings(input)
                    if (results.isEmpty()) sendMessage("no death counts stored for input: $input")
                    else
                    {
                        val stringBuilder = StringBuilder()
                        results.forEach {
                            stringBuilder.append(it)
                            stringBuilder.appendLine()
                        }
                        sendMessage(stringBuilder.trim().toString())
                    }
                }

                startsWith("!playtime") ->
                {
                    val input = this.removePrefix("!playtime").trim()
                    val result = PlaytimeCommand.getPlaytimeLeaderboardStrings(input)
                    if (result.isEmpty()) sendMessage("no playtime tracked for input: $input")
                    val stringBuilder = StringBuilder()
                    result.forEach {
                        stringBuilder.append(it)
                        stringBuilder.appendLine()
                    }
                    sendMessage(stringBuilder.trim().toString())
                }

                else ->
                {
                    val authorName = event.member?.displayName ?: event.message.author?.username ?: "NONAME"
                    MinecraftHandler.sendMessage(authorName, event.message.content)
                }
            }
        }
    }

    fun sendMessage(text: String)
    {
        Main.scope.launch {
            if (text.isBlank()) return@launch
            channel.createMessage(text)
        }
    }

    private suspend fun registerUser(userId: Snowflake, minecraftName: String)
    {
        val member = guild.getMemberOrNull(userId)
        if (member == null)
        {
            handleNotAMember(userId)
            return
        }
        val oldName = Users.getValueForKey(member).orEmpty()
        if (!Users.registerUser(member, minecraftName))
        {
            sendMessage("$minecraftName is already assigned to ${getPrettyMemberName(member)}")
        } else
        {
            MinecraftHandler.runWhitelistRemove(oldName)
            MinecraftHandler.runWhitelistAdd(minecraftName)
            sendMessage(
                "$minecraftName now assigned to ${getPrettyMemberName(member)}\n" +
                        "$minecraftName is now whitelisted" +
                        if (oldName.isNotEmpty()) "\n$oldName is no longer whitelisted" else ""
            )
        }
        saveUsersToFile()
    }

    //TODO: load every so often
    suspend fun loadUsersFromFile()
    {
        Users.clear()

        Config.configEntry.users.forEach { (discordId, minecraftName) ->
            val memberSnowflake = Snowflake(discordId)
            val member = guild.getMemberOrNull(memberSnowflake)
            if (member != null) Users.put(member, minecraftName)
            else handleNotAMember(memberSnowflake)
        }
    }

    private fun saveUsersToFile()
    {
        Config.configEntry.users = Users.getAsUserEntryList().toList()
        Config.store()
    }

    private fun printOnlinePlayers()
    {
        val onlinePlayers = MinecraftHandler.getOnlinePlayers()
        val sb = StringBuilder("Currently online: ")
        if (onlinePlayers.isEmpty()) sb.append("~nobody~, ")
        else onlinePlayers.forEach { sb.append("$it, ") }
        sb.deleteRange(sb.length - 2, sb.length)
        sendMessage(sb.toString())
    }

    private fun printRegisteredUsers()
    {
        val sb = StringBuilder("Currently registered Users:")
        Users.getAsUserList().forEach {
            sb.appendLine()
            sb.append(it.prettyComboName)
        }
        sendMessage(sb.toString())
    }

    private suspend fun ensureWhitelist(snowflake: Snowflake)
    {
        val member = guild.getMemberOrNull(snowflake)
        if (member == null)
        {
            handleNotAMember(snowflake)
            return
        }
        if (!Users.containsKey(member)) sendMessage("Please register first with `!register minecraftName`")
        else
        {
            val minecraftName = Users.getValueForKey(member).orEmpty()
            MinecraftHandler.runWhitelistAdd(minecraftName)
            sendMessage("Ensured whitelist for $minecraftName") //TODO: reaction
        }
    }

    private fun sendHelpText()
    {
        val helpString =
            "Available commands:\n" +
                    "- `!register minecraftName`: connect your Minecraft name to your Discord account\n" +
                    "- `!whitelist`: ensure that you're on the whitelist if it doesn't automatically work\n" +
                    "\n" +
                    "- `!users`: see all registered Users\n" +
                    "- `!whois username`: look for a user\n" +
                    "- `!updatenames`: update discord display names in database\n" +
                    "\n" +
                    "- `!list`: list currently online players\n" +
                    "- `!deaths minecraftName`: shows how often a player has died\n" +
                    "- `!playtime minecraftName`: shows a players playtime\n" +
                    "\n" +
                    "- `!help`: see this help text"
        sendMessage(helpString)
    }

    private fun printPerfMetrics()
    {
        val performanceMetrics = MinecraftHandler.getPerformanceMetrics()
        sendMessage("TPS: ${performanceMetrics.tps.trim(1)} MSPT: ${performanceMetrics.mspt.trim(1)}")
    }

    private fun handleNotAMember(id: Snowflake)
    {
        sendMessage("ERROR: user with id ${id.value} is not a member of configured guild!")
    }

    fun String.markdownSafe(): String
    {
        return this
            .replace("\\", "\\\\")
            .replace("*", "\\*")
            .replace("_", "\\_")
            .replace("~", "\\~")
            .replace("`", "\\`")
            .replace("|", "\\|")
            .replace(">", "\\>")
    }

    private fun getPrettyMemberName(member: Member): String
    {
        return "@${member.username} (${member.displayName})"
    }

    fun String.convertMentions(): String // TODO: integrate
    {
        var newString = this
        val matcher = Pattern.compile("@.*@").matcher(this)
        while (matcher.find())
        {
            val mention = matcher.group().substring(1, length - 1)
            val member = Users.getDiscordMember(mention)
            if (member != null) newString = newString.replaceRange(matcher.start(), matcher.end(), "<@!${member.id.value}>")
        }
        return newString
    }
}