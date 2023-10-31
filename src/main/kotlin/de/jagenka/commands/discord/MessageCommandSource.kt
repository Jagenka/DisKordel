package de.jagenka.commands.discord

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import de.jagenka.DiscordHandler
import dev.kord.core.Kord
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.event.message.MessageCreateEvent


class MessageCommandSource(private val event: MessageCreateEvent)
{
    fun sendCodeBlock(text: String)
    {
        DiscordHandler.sendCodeBlock(text = text, silent = true)
    }

    val author: User?
        get() = event.message.author

    val message: Message
        get() = event.message

    val kord: Kord
        get() = event.kord

    companion object
    {
        fun literal(name: String): LiteralArgumentBuilder<MessageCommandSource>
        {
            return LiteralArgumentBuilder.literal(name)
        }

        fun <T> argument(name: String?, type: ArgumentType<T>?): RequiredArgumentBuilder<MessageCommandSource, T>
        {
            return RequiredArgumentBuilder.argument(name, type)
        }

        /**
         * this method is adapted from [here](https://github.com/PaperMC/Velocity/blob/8abc9c80a69158ebae0121fda78b55c865c0abad/proxy/src/main/java/com/velocitypowered/proxy/util/BrigadierUtils.java#L38),
         * as Mojang seems to not address this [issue](https://github.com/Mojang/brigadier/issues/46)
         */
        fun redirect(alias: String, destination: LiteralCommandNode<MessageCommandSource>): LiteralArgumentBuilder<MessageCommandSource>
        {
            val builder = literal(alias)
                .requires(destination.requirement)
                .forward(destination.redirect, destination.redirectModifier, destination.isFork)
                .executes(destination.command)
            destination.children.forEach { builder.then(it) }
            return builder
        }
    }
}