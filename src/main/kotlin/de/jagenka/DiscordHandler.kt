package de.jagenka

import de.jagenka.commands.discord.*
import de.jagenka.commands.discord.structure.Registry
import de.jagenka.config.Config
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.ReactionEmoji
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.x.emoji.Emojis
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

            registerCommands()

            Registry.setup(kord)

            kord.login {// nicht sicher ob man für jeden link nen eigenen bot braucht mit der API
                @OptIn(PrivilegedIntent::class)
                intents += Intent.MessageContent
            }
        } ?: error("error initializing bot")
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
            register(TestCommand)
        }
    }

    fun sendMessage(text: String)
    {
        Main.scope.launch {
            if (text.isBlank()) return@launch
            channel.createMessage(text)
        }
    }

    //TODO: load every so often
    suspend fun loadUsersFromFile()
    {
        UserRegistry.clear()

        Config.configEntry.users.forEach { (discordId, minecraftName) ->
            val memberSnowflake = Snowflake(discordId)
            val member = guild.getMemberOrNull(memberSnowflake)
            if (member != null) UserRegistry.put(member, minecraftName)
            else handleNotAMember(memberSnowflake)
        }
    }

    private suspend fun ensureWhitelist(snowflake: Snowflake)
    {
        val member = guild.getMemberOrNull(snowflake)
        if (member == null)
        {
            handleNotAMember(snowflake)
            return
        }
        if (!UserRegistry.containsKey(member)) sendMessage("Please register first with `!register minecraftName`")
        else
        {
            val minecraftName = UserRegistry.getValueForKey(member).orEmpty()
            MinecraftHandler.runWhitelistAdd(minecraftName)
            sendMessage("Ensured whitelist for $minecraftName") //TODO: reaction
        }
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

    fun handleNotAMember(id: Snowflake)
    {
        sendMessage("ERROR: user with id ${id.value} is not a member of configured guild!")
    }

    fun reactConfirmation(message: Message)
    {
        Main.scope.launch {
            message.addReaction(ReactionEmoji.Unicode(Emojis.whiteCheckMark.unicode))
        }
    }

    fun String.convertMentions(): String // TODO: integrate
    {
        var newString = this
        val matcher = Pattern.compile("@.*@").matcher(this)
        while (matcher.find())
        {
            val mention = matcher.group().substring(1, length - 1)
            val member = UserRegistry.getDiscordMember(mention)
            if (member != null) newString = newString.replaceRange(matcher.start(), matcher.end(), "<@!${member.id.value}>")
        }
        return newString
    }

    fun getPrettyMemberName(member: Member): String
    {
        return "@${member.username} (${member.displayName})"
    }
}