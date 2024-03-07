package de.jagenka.config

import de.jagenka.MinecraftUser
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

@Serializable
class DiscordSettingsEntry(
    var botToken: String = "BOT_TOKEN",
    var guildId: Long = 123456789,
    var channelId: Long = 123456789,
    var serverName: String = "Server",
    var serverIconURL: String? = "URL_TO_PNG",
    var playerAvatarsURL: String? = "URL_TO_PNG/%uuid%"
)

@Serializable
data class UserEntry(
    var discordId: Long = 123456789,
    var minecraftName: String = "Herobrine",
)

object MinecraftUserSerializer : KSerializer<MinecraftUser>
{
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("MinecraftUser", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: MinecraftUser)
    {
        encoder.encodeString("${value.name} ${value.uuid} ${value.skinURL} ${value.lastURLUpdate}")
    }

    override fun deserialize(decoder: Decoder): MinecraftUser
    {
        val columns = decoder.decodeString().split(" ")
        val name = columns.getOrNull(0) ?: ""
        val uuid = columns.getOrNull(1) ?: ""
        val url = columns.getOrNull(2) ?: ""
        val lastUpdate = columns.getOrNull(3) ?: "0"
        return MinecraftUser(name, UUID.fromString(uuid), url, lastUpdate.toLong())
    }
}

/**
 * @param discordCommandCache map of internal id to Discord id
 */
@Serializable
class BaseConfigEntry(
    var discordSettings: DiscordSettingsEntry = DiscordSettingsEntry(),
    var registeredUsers: MutableList<UserEntry> = mutableListOf(),
    var userCache: MutableSet<MinecraftUser> = mutableSetOf(),
    var appCommandVersion: String = "0",
)