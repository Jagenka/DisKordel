package de.jagenka.config

import de.jagenka.InvalidConfigException
import de.jagenka.MissingConfigException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

object Config
{
    private lateinit var pathToConfigFile: Path

    private val serializer = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    lateinit var configEntry: BaseConfigEntry

    fun loadConfig()
    {
        pathToConfigFile = FabricLoader.getInstance().configDir.resolve("diskordel.json")
        if (!Files.exists(pathToConfigFile))
        {
            Files.createFile(pathToConfigFile)
            Files.writeString(pathToConfigFile, serializer.encodeToString(BaseConfigEntry()))

            throw MissingConfigException("Config file was missing. Please enter bot token, guild id and channel id under discordSettings into ${pathToConfigFile.fileName}.")
        }
        try
        {
            configEntry = serializer.decodeFromString(pathToConfigFile.toFile().readText())
        } catch (_: Exception)
        {
            throw InvalidConfigException("Error loading config. Please enter valid data under discordSettings. Delete file for new template.")
        }
    }

    fun store()
    {
        val json = serializer.encodeToString(configEntry)
        Files.writeString(pathToConfigFile, json)
    }
}