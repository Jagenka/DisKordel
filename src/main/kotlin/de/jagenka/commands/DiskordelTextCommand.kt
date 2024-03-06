package de.jagenka.commands

import com.mojang.brigadier.CommandDispatcher
import de.jagenka.commands.discord.MessageCommandSource

interface DiskordelTextCommand : DiskordelCommand
{
    // TODO: needs admin

    /**
     * help text shown in help command overview
     */
    val shortHelpText: String

    /**
     * help text shown when querying a specific command in help command
     */
    val longHelpText: String

    /**
     * this should register the command with the bots command registry
     */
    fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
}