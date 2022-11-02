package de.jagenka.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

object StatManager
{
    private lateinit var pathToStatFile: Path

    private val serializer = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    lateinit var statEntries: MutableMap<String, StatEntry>

    fun loadStats()
    {
        pathToStatFile = FabricLoader.getInstance().configDir.resolve("hackfleisch-diskurs-stats.json")
        if (!Files.exists(pathToStatFile))
        {
            Files.createFile(pathToStatFile)
            Files.writeString(pathToStatFile, serializer.encodeToString(mapOf<String, StatEntry>()))
        }
        statEntries = serializer.decodeFromString(pathToStatFile.toFile().readText())
    }

    fun store()
    {
        val json = serializer.encodeToString(statEntries)
        Files.writeString(pathToStatFile, json)
    }
}

@Serializable
class StatEntry(
    var deaths: Int = 0
)