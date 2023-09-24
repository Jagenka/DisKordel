package de.jagenka

import dev.kord.common.Color
import dev.kord.common.entity.optional.OptionalInt
import dev.kord.core.cache.data.*
import dev.kord.core.entity.Embed
import dev.kord.rest.builder.message.EmbedBuilder
import java.nio.file.Path
import dev.kord.common.entity.DiscordWebhook
import dev.kord.common.entity.Snowflake
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

    /**
     * Complete class for paginated Embeds.
     */
    class EmbedCatalogue
    {
        val catalogue: Catalogue = Catalogue()

        /**
         * Pagination system for CataloguePages.
         */
        data class Catalogue(val pages: MutableList<CataloguePage> = mutableListOf(), var index: Int = 0)
        {
            fun currentPage(): CataloguePage
            {
                return pages[index]
            }

            fun nextPage(): CataloguePage
            {
                if (index < pages.lastIndex) return pages[++index]
                return currentPage()
            }

            fun previousPage(): CataloguePage
            {
                if (index > 0) return pages[--index]
                return currentPage()
            }

            fun gotoPage(desiredIndex: Int): CataloguePage
            {
                if (desiredIndex in 0..pages.lastIndex) index = desiredIndex
                return currentPage()
            }
        }

        /**
         * A wrapper for EmbedBuilder which also part of the implementation of a pagination system for easy use of discord embeds.
         */
        data class CataloguePage(val embedBuilder: EmbedBuilder = EmbedBuilder(), val files: MutableMap<String, Path> = mutableMapOf())
        {
            var data = EmbedData(
                title = dev.kord.common.entity.optional.Optional.Missing(),
                type = dev.kord.common.entity.optional.Optional.Missing(),
                description = dev.kord.common.entity.optional.Optional.Missing(),
                url = dev.kord.common.entity.optional.Optional.Missing(),
                timestamp = dev.kord.common.entity.optional.Optional.Missing(),
                color = OptionalInt.Value(Color(255, 0, 0).rgb),
                footer = dev.kord.common.entity.optional.Optional.Missing(),
                image = dev.kord.common.entity.optional.Optional.Missing(),
                thumbnail = dev.kord.common.entity.optional.Optional.Missing(),
                video = dev.kord.common.entity.optional.Optional.Missing(),
                provider = dev.kord.common.entity.optional.Optional.Missing(),
                author = dev.kord.common.entity.optional.Optional.Missing(),
                fields = dev.kord.common.entity.optional.Optional.Missing()
            )

            fun setTitle(title: String)
            {
                data = data.copy(title = dev.kord.common.entity.optional.Optional.Value(title))
            }

            fun setDescription(text: String)
            {
                data = data.copy(description = dev.kord.common.entity.optional.Optional.Value(text))
            }

            fun setURL(url: String)
            {
                data = data.copy(url = dev.kord.common.entity.optional.Optional.Value(url))
            }

            fun setColor(r: Int, g: Int, b: Int)
            {
                data = data.copy(color = OptionalInt.Value(Color(r, g, b).rgb))
            }

            fun setFooter(text: String, iconPath: Path)
            {
                val footerData = EmbedFooterData(
                    text = text,
                    iconUrl = dev.kord.common.entity.optional.Optional.Value("attachment://${iconPath.fileName}"),
                    proxyIconUrl = dev.kord.common.entity.optional.Optional.Missing()
                )
                data = data.copy(footer = dev.kord.common.entity.optional.Optional.Value(footerData))
                files["Icon"] = iconPath
            }

            fun setImage(path: Path)
            {
                val imageData = EmbedImageData(
                    url = dev.kord.common.entity.optional.Optional.Value("attachment://${path.fileName}"),
                    proxyUrl = dev.kord.common.entity.optional.Optional.Missing(),
                    height = OptionalInt.Missing,
                    width = OptionalInt.Missing
                )
                data = data.copy(image = dev.kord.common.entity.optional.Optional.Value(imageData))
                files["Image"] = path
            }

            fun setImageAsURL(url: String)
            {
                val imageData = EmbedImageData(
                    url = dev.kord.common.entity.optional.Optional.Value(url),
                    proxyUrl = dev.kord.common.entity.optional.Optional.Missing(),
                    height = OptionalInt.Missing,
                    width = OptionalInt.Missing
                )
                data = data.copy(image = dev.kord.common.entity.optional.Optional.Value(imageData))
            }

            fun setThumbnail(path: Path)
            {
                val thumbnailData = EmbedThumbnailData(
                    url = dev.kord.common.entity.optional.Optional.Value("attachment://${path.fileName}"),
                    proxyUrl = dev.kord.common.entity.optional.Optional.Missing(),
                    height = OptionalInt.Missing,
                    width = OptionalInt.Missing
                )
                data = data.copy(thumbnail = dev.kord.common.entity.optional.Optional.Value(thumbnailData))
                files["Thumbnail"] = path
            }

            fun setThumbnailAsURL(url: String)
            {
                val thumbnailData = EmbedThumbnailData(
                    url = dev.kord.common.entity.optional.Optional.Value(url),
                    proxyUrl = dev.kord.common.entity.optional.Optional.Missing(),
                    height = OptionalInt.Missing,
                    width = OptionalInt.Missing
                )
                data = data.copy(thumbnail = dev.kord.common.entity.optional.Optional.Value(thumbnailData))
            }

            fun setVideo(url: String)
            {
                val videoData = EmbedVideoData(
                    url = dev.kord.common.entity.optional.Optional.Value(url),
                    height = OptionalInt.Missing,
                    width = OptionalInt.Missing
                )
                data = data.copy(video = dev.kord.common.entity.optional.Optional.Value(videoData))
            }

            fun setProvider(name: String, url: String = "")
            {
                val providerData = EmbedProviderData(
                    name = dev.kord.common.entity.optional.Optional.Value(name),
                    url = if (url.isNotBlank()) dev.kord.common.entity.optional.Optional.Value(url) else dev.kord.common.entity.optional.Optional.Missing()
                )
                data = data.copy(provider = dev.kord.common.entity.optional.Optional.Value(providerData))
            }

            fun setAuthor(author: String, url: String = "")
            {
                val authorData = EmbedAuthorData(
                    name = dev.kord.common.entity.optional.Optional.Value(author),
                    url = if (url.isNotBlank()) dev.kord.common.entity.optional.Optional.Value(url) else dev.kord.common.entity.optional.Optional.Missing(),
                    iconUrl = dev.kord.common.entity.optional.Optional.Missing(),
                    proxyIconUrl = dev.kord.common.entity.optional.Optional.Missing(),
                )
                data = data.copy(author = dev.kord.common.entity.optional.Optional.Value(authorData))
            }

            fun addFields(vararg field: EmbedFieldData)
            {
                val fields = mutableListOf<EmbedFieldData>()
                data.fields.value?.let {
                    fields.addAll(it)
                }
                fields.addAll(field)
                data = data.copy(fields = dev.kord.common.entity.optional.Optional.Value(fields))
            }

            /**
             * Applies all changed values to the embedBuilder.
             */
            fun apply()
            {
                DiscordHandler.kord?.let { kord ->
                    Embed(data, kord).apply(embedBuilder)
                } ?: error("Kord not found.")
            }
        }
    }
}