package de.jagenka.config

import kotlinx.serialization.Serializable

@Serializable
class BotConfigEntry(
    var botToken: String = "BOT_TOKEN",
    var guildId: Long = 123456789,
    var channelId: Long = 123456789
)

@Serializable
data class UserEntry(
    var discordId: Long = 123456789,
    var minecraftName: String = "Herobrine"
)

@Serializable
class BaseConfigEntry(
    var discordSettings: BotConfigEntry = BotConfigEntry(),
    var users: List<UserEntry> = listOf()
)