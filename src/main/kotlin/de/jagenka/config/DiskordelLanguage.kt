package de.jagenka.config

import com.google.common.collect.ImmutableMap
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import de.jagenka.MinecraftHandler
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.text.*
import net.minecraft.util.JsonHelper
import net.minecraft.util.Language
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.BiConsumer
import java.util.regex.Pattern
import kotlin.io.path.inputStream

object DiskordelLanguage : Language()
{
    private val GSON = Gson()
    private val TOKEN_PATTERN: Pattern = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]")
    private val map: ImmutableMap<String, String>

    init
    {
        val builder = ImmutableMap.builder<String, String>()
        val biConsumer = BiConsumer { key: String, value: String -> builder.put(key, value) }
        val path = FabricLoader.getInstance().configDir.resolve("diskordel_lang.json")
        try
        {
            path.inputStream().let { inputStream ->
                val jsonObject = GSON.fromJson(
                    InputStreamReader(inputStream, StandardCharsets.UTF_8) as Reader,
                    JsonObject::class.java
                )
                for ((key, value) in jsonObject.entrySet())
                {
                    val string = TOKEN_PATTERN.matcher(JsonHelper.asString(value, key)).replaceAll("%$1s")
                    biConsumer.accept(key, string)
                }
            }
        } catch (exception: JsonParseException)
        {
            MinecraftHandler.logger.error("Couldn't read strings from {}", path)
        } catch (exception: IOException)
        {
            MinecraftHandler.logger.error("Couldn't read strings from {}", path)
        }
        map = builder.build()
    }

    override fun get(key: String?, fallback: String?) = map.getOrDefault(key, fallback)

    override fun hasTranslation(key: String?) = map.containsKey(key)

    override fun isRightToLeft() = false

    override fun reorder(text: StringVisitable?): OrderedText
    {
        return OrderedText { visitor: CharacterVisitor? ->
            text!!.visit({ style: Style?, string: String? ->
                if (TextVisitFactory.visitFormatted(
                        string,
                        style,
                        visitor
                    )
                ) Optional.empty() else StringVisitable.TERMINATE_VISIT
            }, Style.EMPTY).isPresent
        }
    }
}