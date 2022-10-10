package de.jagenka

import de.jagenka.Util.trim
import de.jagenka.commands.DeathsCommand.getDeathLeaderboardStrings
import de.jagenka.commands.PlaytimeCommand.getPlaytimeLeaderboardStrings
import de.jagenka.config.Config
import de.jagenka.config.Config.configEntry
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.entity.Member
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.on
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.util.regex.Pattern

object ChannelHandler
{
    private val channels = mutableListOf<LinkedChannel>()

    init
    {
        Main.kord?.let { kord ->
            kord.on<MessageCreateEvent> {
                // return if wrong guild
                if (message.getGuildOrNull()?.id !in channels.map { it.guild.id }) return@on
                // return if wrong channel
                if (message.channelId !in channels.map { it.channel.id }) return@on
                // return if author is a bot or undefined
                if (message.author?.isBot != false) return@on

                // TODO: handle chat messages here
            }
        }
    }

    fun addChannel(guildSnowflake: Snowflake, channelSnowflake: Snowflake)
    {
        Main.scope.launch {
            repeat(5) {
                Main.kord?.let { kord ->
                    channels.add(
                        LinkedChannel(
                            channel = MessageChannelBehavior(channelSnowflake, kord),
                            guild = GuildBehavior(guildSnowflake, kord)
                        )
                    )

                    return@launch // we're done here
                }
                delay(1000)
            }
            error("error connecting to channel $channelSnowflake in guild $guildSnowflake")
        }
    }

    @JvmStatic
    fun handleChatMessage(message: Text, sender: ServerPlayerEntity?)
    {
        sendMessage("<${sender?.name?.string}> ${message.string.asDiscordMarkdownSafe()}")
    }

    @JvmStatic
    fun handleSystemMessage(message: Text)
    {
        if (message.string.startsWith(">")) return
        sendMessage(message.string.asDiscordMarkdownSafe())
    }

    private fun String.asDiscordMarkdownSafe(): String
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

    private suspend fun registerUser(userId: Snowflake, minecraftName: String)
    {
        val member = guild?.getMember(userId)
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
            Main.runWhitelistRemove(oldName)
            Main.runWhitelistAdd(minecraftName)
            sendMessage(
                "$minecraftName now assigned to ${getPrettyMemberName(member)}\n" +
                        "$minecraftName is now whitelisted" +
                        if (oldName.isNotEmpty()) "\n$oldName is no longer whitelisted" else ""
            )
        }
        saveUsersToFile()
    }

    private fun saveUsersToFile()
    {
        configEntry.users = Users.getAsUserEntryList().toList()
        Config.store()
    }

    private fun sendMessageToMinecraft(sender: String, text: String)
    {
        Util.sendChatMessage(Text.literal(">$sender< $text").getWithStyle(Style.EMPTY.withFormatting(Formatting.BLUE))[0])
    }

    private fun String.convertMentions(): String
    {
        var newString = this
        val matcher = Pattern.compile("@.*@").matcher(this)
        while (matcher.find())
        {
            val mention = matcher.group().substring(1, length - 1)
            val member = Users.getDiscordMember(mention)
            if (member != null) newString = newString.replaceRange(matcher.start(), matcher.end(), "<@!${member.id.asLong()}>")
        }
        return newString
    }

    private suspend fun printOnlinePlayers()
    {
        val onlinePlayers = Main.getOnlinePlayers()
        val sb = StringBuilder("Currently online: ")
        if (onlinePlayers.isEmpty()) sb.append("~nobody~, ")
        else onlinePlayers.forEach { sb.append("$it, ") }
        sb.deleteRange(sb.length - 2, sb.length)
        sendMessage(sb.toString())
    }

    private suspend fun sendRegisteredUsersToChat()
    {
        val sb = StringBuilder("Currently registered Users:")
        Users.getAsUserList().forEach {
            sb.appendLine()
            sb.append(getPrettyComboName(it))
        }
        sendMessage(sb.toString())
    }

    private suspend fun ensureWhitelist(snowflake: Snowflake)
    {
        val member = guild?.getMember(snowflake)
        if (member == null)
        {
            handleNotAMember(snowflake)
            return
        }
        if (!Users.containsKey(member)) sendMessage("Please register first with `!register minecraftName`")
        else
        {
            val minecraftName = Users.getValueForKey(member).orEmpty()
            Main.runWhitelistAdd(minecraftName)
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

    fun whoIsUser(name: String): String
    {
        val members = Users.find(name)
        return if (members.isEmpty())
        {
            "No users found!"
        } else
        {
            val sb = StringBuilder("")
            members.forEach {
                sb.append(getPrettyComboName(it))
                sb.appendLine()
            }
            sb.setLength(sb.length - 1)
            sb.toString()
        }
    }

    private fun getPrettyComboName(user: de.jagenka.User): String
    {
        return "${user.username} (${user.displayName}) aka ${user.minecraftName}"
    }

    private fun handlePerfCommand()
    {
        val performanceMetrics = Main.getPerformanceMetrics()
        sendMessage("TPS: ${performanceMetrics.tps.trim(1)} MSPT: ${performanceMetrics.mspt.trim(1)}")
    }

    private fun processMessage(message: Message)
    {
        with(message.content)
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
                    registerUser(message.author.get().id, this.removePrefix("!register").trim())
                }

                equals("!users") ->
                {
                    sendRegisteredUsersToChat()
                }

                startsWith("!whois") ->
                {
                    val input = this.removePrefix("!whois").trim()
                    sendMessage(whoIsUser(input))
                }

                equals("!updatenames") ->
                {
                    loadUsersFromFile()
                    //TODO: add reaction
                }

                startsWith("!cmd") -> //NOT FILTERED!!
                {
                    if (isJay(message.author.get())) HackfleischDiskursMod.runCommand(this.removePrefix("!cmd").trim())
                }

                startsWith("!whitelist") ->
                {
                    ensureWhitelist(message.author.get().id)
                }

                equals("!perf") ->
                {
                    handlePerfCommand()
                }

                startsWith("!deaths") ->
                {
                    val input = this.removePrefix("!deaths").trim()
                    val results = getDeathLeaderboardStrings(input)
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
                    val result = getPlaytimeLeaderboardStrings(input)
                    if (result.isEmpty()) sendMessage("no playtime tracked for input: $input")
                    val stringBuilder = StringBuilder()
                    result.forEach {
                        stringBuilder.append(it)
                        stringBuilder.appendLine()
                    }
                    sendMessage(stringBuilder.trim().toString())
                }

                equals("!thing") -> HackfleischDiskursMod.doThing()
                else ->
                {
                    val member = gateway.getMemberById(guildId, message.author.get().id).block()
                    sendMessageToMinecraft(if (member != null) member.displayName else message.author.get().username, message.content)
                }
            }
        }
    }

    private fun isJay(user: User): Boolean
    {
        return user.id == Snowflake.of(174579897795084288)
    }
}
