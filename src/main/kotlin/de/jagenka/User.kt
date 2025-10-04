package de.jagenka

import de.jagenka.Util.unwrap
import de.jagenka.config.Config
import de.jagenka.config.MinecraftUserSerializer
import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.net.URI
import java.util.*
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.hours

data class User(val discord: DiscordUser, val minecraft: MinecraftUser)
{
    fun isLikely(name: String): Boolean
    {
        val discordMembers = UserRegistry.getDiscordMembers()
        return this.minecraft.username.contains(name, ignoreCase = true)
                || discordMembers[this.discord]?.username?.contains(name, ignoreCase = true) ?: false
                || discordMembers[this.discord]?.effectiveName?.contains(name, ignoreCase = true) ?: false
    }

    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        return discord == other.discord
    }

    override fun hashCode(): Int
    {
        return discord.hashCode()
    }
}

@Serializable(with = MinecraftUserSerializer::class)
data class MinecraftUser(var username: String, var uuid: UUID, var skinURL: String = "", var lastURLUpdate: Long = 0)
{
    suspend fun getSkinURL(): String
    {
        val playerAvatarsURL = Config.configEntry.discordSettings.playerAvatarsURL
        if (playerAvatarsURL != null)
        {
            val replacedURL = playerAvatarsURL.replace("%uuid%", uuid.toString().replace("-", ""))

            try
            {
                val uri = URI(replacedURL)
                return uri.toURL().toString()
            } catch (_: Exception)
            {

            }
        }

        // fallback, if above parsing does not work
        updateSkin()
        return this.skinURL

    }

    private suspend fun updateSkin()
    {
        if (skinURL.isBlank() || System.currentTimeMillis() > lastURLUpdate + 4.hours.inWholeMilliseconds)
        {
            MinecraftHandler.minecraftServer?.apply {

                val profile = apiServices.profileResolver.getProfileById(uuid)?.unwrap() ?: MinecraftHandler.logger.error("no profile found for UUID $uuid").run { return }
                val texture = apiServices.sessionService.getTextures(profile).skin ?: return

                val skin = ImageIO.read(URI(texture.url).toURL())
                val layer1 = skin.getSubimage(8, 8, 8, 8)
                val layer2 = skin.getSubimage(40, 8, 8, 8)
                val head = BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB)
                val g = head.createGraphics()
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
                g.drawImage(layer1, 4, 4, 40, 40, null)
                g.drawImage(layer2, 0, 0, 48, 48, null)

                val imageMessage = DiscordHandler.sendImage("${profile.id}.png", head, silent = true)
                val imageUrl = imageMessage.data.attachments.first().url
                imageMessage.delete()

                skinURL = imageUrl
                lastURLUpdate = System.currentTimeMillis()

                Main.scope.launch {
                    UserRegistry.saveCacheToFile()
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean
    {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MinecraftUser

        return uuid == other.uuid
    }

    override fun hashCode(): Int
    {
        return uuid.hashCode()
    }
}

data class DiscordUser(val id: Snowflake)