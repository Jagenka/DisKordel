package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.UserRegistry
import de.jagenka.commands.discord.structure.ArgumentCombination
import de.jagenka.commands.discord.structure.ArgumentCombination.Companion.empty
import de.jagenka.commands.discord.structure.MessageCommand

object UpdateNamesCommand : MessageCommand
{
    override val ids: List<String>
        get() = listOf("updatenames")
    override val helpText: String
        get() = "Update Member names in registry."
    override val allowedArgumentCombinations: List<ArgumentCombination>
        get() = listOf(empty(helpText) { event ->
            UserRegistry.loadRegisteredUsersFromFile()
            DiscordHandler.reactConfirmation(event.message)
            true
        })
}