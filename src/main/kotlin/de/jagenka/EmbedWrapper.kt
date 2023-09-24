package de.jagenka

import dev.kord.common.Color
import dev.kord.common.entity.optional.Optional
import dev.kord.common.entity.optional.OptionalInt
import dev.kord.core.cache.data.*
import dev.kord.core.entity.Embed
import dev.kord.rest.builder.message.EmbedBuilder
import java.nio.file.Path

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
     * Example:
     * private fun createExamplePage(): CataloguePage
     *     {
     *         val page = CataloguePage()
     *
     *         page.setTitle("This is an example page")
     *         page.setDescription("This is an example description")
     *         page.setImage(Path("<image path>"))
     *         page.setFooter("This is an example footer")
     *
     *         page.apply()
     *         return page
     *     }
     */
    data class CataloguePage(val embedBuilder: EmbedBuilder = EmbedBuilder(), val files: MutableMap<String, Path> = mutableMapOf())
    {
        var data = EmbedData(
            title = Optional.Missing(),
            type = Optional.Missing(),
            description = Optional.Missing(),
            url = Optional.Missing(),
            timestamp = Optional.Missing(),
            color = OptionalInt.Value(Color(255, 0, 0).rgb),
            footer = Optional.Missing(),
            image = Optional.Missing(),
            thumbnail = Optional.Missing(),
            video = Optional.Missing(),
            provider = Optional.Missing(),
            author = Optional.Missing(),
            fields = Optional.Missing()
        )

        //region Setters
        fun setTitle(title: String)
        {
            data = data.copy(title = Optional.Value(title))
        }

        fun setDescription(text: String)
        {
            data = data.copy(description = Optional.Value(text))
        }

        fun setURL(url: String)
        {
            data = data.copy(url = Optional.Value(url))
        }

        fun setColor(r: Int, g: Int, b: Int)
        {
            data = data.copy(color = OptionalInt.Value(Color(r, g, b).rgb))
        }

        fun setFooter(text: String, iconPath: Path)
        {
            val footerData = EmbedFooterData(
                text = text,
                iconUrl = Optional.Value("attachment://${iconPath.fileName}"),
                proxyIconUrl = Optional.Missing()
            )
            data = data.copy(footer = Optional.Value(footerData))
            files["Icon"] = iconPath
        }

        fun setImage(path: Path)
        {
            val imageData = EmbedImageData(
                url = Optional.Value("attachment://${path.fileName}"),
                proxyUrl = Optional.Missing(),
                height = OptionalInt.Missing,
                width = OptionalInt.Missing
            )
            data = data.copy(image = Optional.Value(imageData))
            files["Image"] = path
        }

        fun setImageAsURL(url: String)
        {
            val imageData = EmbedImageData(
                url = Optional.Value(url),
                proxyUrl = Optional.Missing(),
                height = OptionalInt.Missing,
                width = OptionalInt.Missing
            )
            data = data.copy(image = Optional.Value(imageData))
        }

        fun setThumbnail(path: Path)
        {
            val thumbnailData = EmbedThumbnailData(
                url = Optional.Value("attachment://${path.fileName}"),
                proxyUrl = Optional.Missing(),
                height = OptionalInt.Missing,
                width = OptionalInt.Missing
            )
            data = data.copy(thumbnail = Optional.Value(thumbnailData))
            files["Thumbnail"] = path
        }

        fun setThumbnailAsURL(url: String)
        {
            val thumbnailData = EmbedThumbnailData(
                url = Optional.Value(url),
                proxyUrl = Optional.Missing(),
                height = OptionalInt.Missing,
                width = OptionalInt.Missing
            )
            data = data.copy(thumbnail = Optional.Value(thumbnailData))
        }

        fun setVideo(url: String)
        {
            val videoData = EmbedVideoData(
                url = Optional.Value(url),
                height = OptionalInt.Missing,
                width = OptionalInt.Missing
            )
            data = data.copy(video = Optional.Value(videoData))
        }

        fun setProvider(name: String, url: String = "")
        {
            val providerData = EmbedProviderData(
                name = Optional.Value(name),
                url = if (url.isNotBlank()) Optional.Value(url) else Optional.Missing()
            )
            data = data.copy(provider = Optional.Value(providerData))
        }

        fun setAuthor(author: String, url: String = "")
        {
            val authorData = EmbedAuthorData(
                name = Optional.Value(author),
                url = if (url.isNotBlank()) Optional.Value(url) else Optional.Missing(),
                iconUrl = Optional.Missing(),
                proxyIconUrl = Optional.Missing(),
            )
            data = data.copy(author = Optional.Value(authorData))
        }

        fun addFields(vararg field: EmbedFieldData)
        {
            val fields = mutableListOf<EmbedFieldData>()
            data.fields.value?.let {
                fields.addAll(it)
            }
            fields.addAll(field)
            data = data.copy(fields = Optional.Value(fields))
        }
        //endregion

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