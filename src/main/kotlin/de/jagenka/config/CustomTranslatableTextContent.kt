package de.jagenka.config

import de.jagenka.MinecraftHandler
import net.minecraft.entity.Entity
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.MutableText
import net.minecraft.text.StringVisitable
import net.minecraft.text.Style
import net.minecraft.text.TranslatableTextContent
import java.util.*

class CustomTranslatableTextContent(key: String?, vararg args: Any?) : TranslatableTextContent(key, null, args)
{
    override fun <T : Any?> visit(visitor: StringVisitable.Visitor<T>?): Optional<T>
    {
        return StringVisitable.plain(DiskordelLanguage.get(key)).visit(visitor)
    }

    override fun <T : Any?> visit(visitor: StringVisitable.StyledVisitor<T>?, style: Style?): Optional<T>
    {
        return StringVisitable.plain(DiskordelLanguage.get(key)).visit(visitor, style)
    }

    override fun parse(source: ServerCommandSource?, sender: Entity?, depth: Int): MutableText
    {
        return MinecraftHandler.customTranslatable(super.parse(source, sender, depth))
    }
}