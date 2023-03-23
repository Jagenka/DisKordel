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
class BotConfigEntry(
    var botToken: String = "BOT_TOKEN",
    var guildId: Long = 123456789,
    var channelId: Long = 123456789
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
        encoder.encodeString("${value.name} ${value.uuid}")
    }

    override fun deserialize(decoder: Decoder): MinecraftUser
    {
        val (name, uuid) = decoder.decodeString().split(" ")
        return MinecraftUser(name, UUID.fromString(uuid))
    }
}

@Serializable
class BaseConfigEntry(
    var discordSettings: BotConfigEntry = BotConfigEntry(),
    var registeredUsers: MutableList<UserEntry> = mutableListOf(),
    var userCache: MutableSet<MinecraftUser> = mutableSetOf(),
)