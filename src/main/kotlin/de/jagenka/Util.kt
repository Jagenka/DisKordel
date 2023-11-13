package de.jagenka

import dev.kord.common.entity.DiscordWebhook
import dev.kord.common.entity.Snowflake
import dev.kord.core.entity.Message
import dev.kord.rest.request.KtorRequestException
import java.util.*

object Util
{
    private val webhookCache = mutableMapOf<String, DiscordWebhook>()

    fun Double.trimDecimals(digits: Int): String
    {
        return "%.${digits}f".format(Locale.US, this)
    }

    fun <T> Optional<T>.unwrap(): T? = orElse(null)

    fun ticksToPrettyString(ticks: Int): String
    {
        val seconds = ticks / 20
        val minutes = seconds / 60
        val hours = minutes / 60

        val sb = StringBuilder()
        if (hours > 0) sb.append("${hours}h")
        if (hours > 0 || minutes > 0) sb.append(" ${minutes - hours * 60}min")
        if (hours > 0 || minutes > 0 || seconds > 0) sb.append(" ${seconds - minutes * 60}s")
        else sb.append("0h 0min 0s")

        return sb.toString().trim()
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

    suspend fun getMessageURL(message: Message): String
    {
        val guildSnowflake = message.getGuildOrNull()?.id ?: return ""
        val channelSnowflake = message.channel.id
        val messageSnowflake = message.id

        return "https://discord.com/channels/$guildSnowflake/$channelSnowflake/$messageSnowflake"
    }
}