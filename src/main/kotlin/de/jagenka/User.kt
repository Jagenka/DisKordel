package de.jagenka

import com.mojang.authlib.minecraft.MinecraftProfileTexture
import de.jagenka.config.MinecraftUserSerializer
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.net.URL
import java.util.*
import javax.imageio.ImageIO
import kotlin.time.Duration.Companion.days

data class User(val discord: DiscordUser, val minecraft: MinecraftUser)
{
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
data class MinecraftUser(val name: String, val uuid: UUID, var skinURL: String = "", var lastURLUpdate: Long = 0)
{
    suspend fun getSkinURL(): String
    {
        updateSkin(this)
        return this.skinURL
    }

    private suspend fun updateSkin(user: MinecraftUser)
    {
        if (user.skinURL.isNotBlank() && System.currentTimeMillis() < user.lastURLUpdate + 1.days.inWholeMilliseconds) return

        val profile = UserRegistry.getGameProfile(user.uuid) ?: return
        MinecraftHandler.minecraftServer?.apply {
            sessionService.fillProfileProperties(profile, false)
            val texture = sessionService.getTextures(profile, false)[MinecraftProfileTexture.Type.SKIN] ?: return

            val skin = ImageIO.read(URL(texture.url))
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

            user.skinURL = imageUrl
            user.lastURLUpdate = System.currentTimeMillis()
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