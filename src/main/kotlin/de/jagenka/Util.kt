package de.jagenka

import de.jagenka.config.Config
import dev.kord.common.entity.DiscordWebhook
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Message
import dev.kord.rest.request.KtorRequestException
import java.net.URI
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object Util
{
    private val webhookCache = mutableMapOf<String, DiscordWebhook>()

    fun Double.trimDecimals(digits: Int): String
    {
        return "%.${digits}f".format(Locale.US, this)
    }

    fun Double.percent(maxDecimals: Int = 2): String
    {
        return (this * 100).trimDecimals(maxDecimals).trimEnd('0').trimEnd('.') + "%"
    }

    fun String.code(): String = "`$this`"

    fun <T> Optional<T>.unwrap(): T? = orElse(null)

    fun ticksToPrettyString(ticks: Int) = durationToPrettyString(ticks.ticks)

    fun durationToPrettyString(duration: Duration): String
    {
        return duration.toComponents { hours, minutes, seconds, _ ->
            var toReturn = ""
            if (hours > 0) toReturn += " ${hours}h"
            if (hours > 0 || minutes > 0) toReturn += " ${minutes}min"
            toReturn += " ${seconds}s"
            return@toComponents toReturn
        }.trim()
    }

    fun findWebhookBySnowflake(id: Snowflake): DiscordWebhook?
    {
        return webhookCache.values.find { it.id == id }
    }

    suspend fun getOrCreateWebhook(name: String): DiscordWebhook
    {
        return webhookCache.getOrPut(name) {
            DiscordHandler.kord?.apply {
                try
                {
                    return@getOrPut rest.webhook.getChannelWebhooks(DiscordHandler.channel.id).find { it.name == name } ?: rest.webhook.createWebhook(
                        DiscordHandler.channel.id,
                        name
                    ) {}
                } catch (_: KtorRequestException)
                {
                    return@getOrPut rest.webhook.createWebhook(DiscordHandler.channel.id, name) {}
                }
            }

            throw KordInstanceMissingException("kord instance missing while getting/creating webhook")
        }
    }

    fun getNewRandomUUID(taken: List<UUID>): UUID
    {
        var uuid = UUID.randomUUID()
        while (uuid in taken) uuid = UUID.randomUUID()
        return uuid
    }

    suspend fun getMessageURL(message: Message?): String
    {
        if (message == null) return ""

        val guildSnowflake = message.getGuildOrNull()?.id ?: return ""
        val channelSnowflake = message.channel.id
        val messageSnowflake = message.id

        return getMessageURL(guildSnowflake, channelSnowflake, messageSnowflake)
    }

    fun getMessageURL(guild: Snowflake?, channel: Snowflake?, message: Snowflake?): String
    {
        if (guild == null) return ""
        var link = "https://discord.com/channels/$guild"
        if (channel == null) return link
        link += "/$channel"
        if (message == null) return link
        link += "/$message"
        return link
    }

    fun getServerIconURL(): String
    {
        val fromConfig = Config.configEntry.discordSettings.serverIconURL ?: ""
        try
        {
            val uri = URI(fromConfig)
            return uri.toURL().toString()
        } catch (e: Exception)
        {
            return ""
        }
    }

    /**
     * Like List.sublist, but limiting to size, when limit >= size. Also defaults to 0, if limit < 0.
     */
    fun <T> List<T>.subListUntilOrEnd(limit: Int): List<T>
    {
        return this.subList(0, min(max(0, limit), this.size))
    }

    val Int.ticks: Duration
        get() = (this / 20.0).seconds
}