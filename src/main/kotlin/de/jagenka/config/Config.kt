package de.jagenka.config

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
        }
        configEntry = serializer.decodeFromString(pathToConfigFile.toFile().readText())
    }

    fun store()
    {
        val json = serializer.encodeToString(configEntry)
        Files.writeString(pathToConfigFile, json)
    }
}