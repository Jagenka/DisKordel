package de.jagenka

import de.jagenka.Util.trim
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClient
import discord4j.core.GatewayDiscordClient
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.Channel
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.rest.RestClient
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Formatting
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.util.*
import java.util.regex.Pattern

//TODO: deaths command
object DiscordBot
{
    var initialized = false

    private val usersFilePath = FabricLoader.getInstance().configDir.resolve("hackfleisch-diskurs-Users.yaml")

    private lateinit var token: String
    private lateinit var guildId: Snowflake
    private lateinit var channel: Channel

    private lateinit var client: DiscordClient
    private lateinit var gateway: GatewayDiscordClient
    private lateinit var restClient: RestClient

    fun initialize(token: String, guildId: Long, channelId: Long) //TODO: catch errors
    {
        this.token = token

        //init DiscordClient
        client = DiscordClient.create(token)
        //init GatewayDiscordClient
        gateway = client.login().block()!!
        //init RestClient
        restClient = gateway.restClient
        //get RestClient AppID
        //var appId = restClient.applicationId.block()!!

        //handle received Messages
        gateway.on(MessageCreateEvent::class.java)
            .filter { event -> event.message.channelId == Snowflake.of(channelId) }
            .filter { event -> !event.message.author.get().isBot }
            .subscribe { event -> processMessage(event.message) }

        this.guildId = Snowflake.of(guildId)
        channel = gateway.getChannelById(Snowflake.of(channelId)).block()!!

        loadUsersFromFile()

        initialized = true
    }

    @JvmStatic
    fun handleSystemMessages(text: String, sender: UUID)
    {
        if (sender == HackfleischDiskursMod.uuid) return
        if (text.startsWith("<") || //TODO mentions?
            text.contains("has made the advancement") ||
            text.contains("has reached the goal") ||
            text.contains("has completed the challenge") ||
            text.contains("joined the game") ||
            text.contains("left the game")
        )
        {
            sendMessage(text.makeDiscordMarkdownSafe())
        }
    }

    private fun String.makeDiscordMarkdownSafe(): String
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

    @JvmStatic
    fun handleDeathMessages(text: String)
    {
        sendMessage(text)
    }

    private fun sendMessage(text: String)
    {
        if (!initialized) return
        if (text.isEmpty() || text.isBlank()) return
        channel.restChannel.createMessage(text).block()
    }

    private fun getPrettyMemberName(member: Member): String
    {
        return "@${member.username} (${member.displayName})"
    }

    private fun handleNotAMember(id: Snowflake)
    {
        sendMessage("ERROR: user with id ${id.asLong()} is not a member of configured guild!")
    }

    private fun registerUser(userId: Snowflake, minecraftName: String)
    {
        val member = gateway.getMemberById(guildId, userId).block()
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
            HackfleischDiskursMod.runWhitelistRemove(oldName)
            HackfleischDiskursMod.runWhitelistAdd(minecraftName)
            sendMessage(
                "$minecraftName now assigned to ${getPrettyMemberName(member)}\n" +
                        "$minecraftName is now whitelisted" +
                        if (oldName.isNotEmpty()) "\n$oldName is no longer whitelisted" else ""
            )
        }
        saveUsersToFile()
    }

    //TODO: load every so often
    private fun loadUsersFromFile()
    {
        Users.clear()
        val confLoader = YamlConfigurationLoader.builder().path(usersFilePath).build()
        val root = confLoader.load()
        repeat(root.childrenList().size) {
            val discordId = root.node(it, "discordId").getLong(0)
            val minecraftName = root.node(it, "minecraftName").getString("")
            val member = gateway.getMemberById(guildId, Snowflake.of(discordId)).block() //lag is jetzt nur noch hier
            if (member != null) Users.put(member, minecraftName)
            else handleNotAMember(Snowflake.of(discordId))
        }
    }

    private fun saveUsersToFile()
    {
        val confLoader = YamlConfigurationLoader.builder().path(usersFilePath).build()
        val root = confLoader.load()
        Users.getAsUsersConfigList().forEachIndexed { index, entry ->
            root.node(index, "discordId").set(entry.discordId)
            root.node(index, "minecraftName").set(entry.minecraftName)
        }
        confLoader.save(root)
    }

    private fun sendMessageToMinecraft(sender: String, text: String)
    {
        HackfleischDiskursMod.broadcastMessage(">$sender< $text", Formatting.BLUE)
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

    private fun printOnlinePlayers()
    {
        val onlinePlayers = HackfleischDiskursMod.getOnlinePlayers()
        val sb = StringBuilder("Currently online: ")
        if (onlinePlayers.isEmpty()) sb.append("~nobody~, ")
        else onlinePlayers.forEach { sb.append("$it, ") }
        sb.deleteRange(sb.length - 2, sb.length)
        sendMessage(sb.toString())
    }

    private fun sendRegisteredUsersToChat()
    {
        val sb = StringBuilder("Currently registered Users:")
        Users.getAsWhoIsOutputList().forEach {
            sb.appendLine()
            sb.append(getPrettyComboName(it))
        }
        sendMessage(sb.toString())
    }

    private fun ensureWhitelist(id: Snowflake)
    {
        val member = gateway.getMemberById(guildId, id).block()
        if (member == null)
        {
            handleNotAMember(id)
            return
        }
        if (!Users.containsKey(member)) sendMessage("Please register first with `!register minecraftName`")
        else
        {
            val minecraftName = Users.getValueForKey(member).orEmpty()
            HackfleischDiskursMod.runWhitelistAdd(minecraftName)
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

    private fun getPrettyComboName(user: WhoIsOutput): String
    {
        return "${user.username} (${user.displayName}) aka ${user.minecraftName}"
    }

    private fun handlePerfCommand()
    {
        val performanceMetrics = HackfleischDiskursMod.getPerformanceMetrics()
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
                    val results = HackfleischDiskursMod.getDeathScore(input)
                    if (results.isEmpty()) sendMessage("$input has no death count stored")
                    else
                    {
                        val stringBuilder = StringBuilder()
                        results.forEach { deathScore ->
                            stringBuilder.append("${deathScore.first} has died ${deathScore.second} time" + if (deathScore.second != 1) "s" else "")
                            stringBuilder.appendLine()
                        }
                        sendMessage(stringBuilder.trim().toString())
                    }
                }
                startsWith("!playtime") ->
                {
                    val input = this.removePrefix("!playtime").trim()
                    val results = HackfleischDiskursMod.getPlaytime(input)
                    if (results.isEmpty()) sendMessage("$input has no playtime tracked")
                    else
                    {
                        val stringBuilder = StringBuilder()
                        results.forEach { playtime ->
                            stringBuilder.append("${playtime.first} has played for ${ticksToPrettyString(playtime.second)}")
                            stringBuilder.appendLine()
                        }
                        sendMessage(stringBuilder.trim().toString())
                    }
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

    private fun ticksToPrettyString(ticks: Int): String
    {
        val seconds = ticks / 20
        val minutes = seconds / 60
        val hours = minutes / 60

        val sb = StringBuilder()
        if (hours > 0) sb.append("${hours}h")
        if (hours > 0 || minutes > 0) sb.append(" ${minutes - hours * 60}min")
        if (hours > 0 || minutes > 0 || seconds > 0) sb.append(" ${seconds - minutes * 60}s")
        else sb.append("0h 0min 0s")

        return sb.toString()
    }

    private fun isJay(user: User): Boolean
    {
        return user.id == Snowflake.of(174579897795084288)
    }
}

@ConfigSerializable
data class UsersConfigEntry(val discordId: Long, val minecraftName: String)
