package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.UserRegistry
import de.jagenka.commands.discord.structure.ArgumentCombination
import de.jagenka.commands.discord.structure.ArgumentCombination.Companion.empty
import de.jagenka.commands.discord.structure.MessageCommand

object UsersCommand : MessageCommand
{
    override val ids: List<String>
        get() = listOf("users")
    override val helpText: String
        get() = "Lists all registered users."
    override val allowedArgumentCombinations: List<ArgumentCombination>
        get() = listOf(empty(helpText) {
            DiscordHandler.sendMessage(UserRegistry.getAllUsersAsOutput())
            true
        })
}