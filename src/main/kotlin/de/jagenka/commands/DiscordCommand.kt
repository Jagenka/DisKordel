package de.jagenka.commands

import com.mojang.brigadier.CommandDispatcher
import de.jagenka.commands.discord.MessageCommandSource

interface DiscordCommand
{
    /**
     * this should register the command with the bots command registry
     */
    fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
}