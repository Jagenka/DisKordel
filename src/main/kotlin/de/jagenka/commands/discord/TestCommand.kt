package de.jagenka.commands.discord

import de.jagenka.UserRegistry
import de.jagenka.commands.discord.structure.ArgumentCombination
import de.jagenka.commands.discord.structure.MessageCommand


object TestCommand : MessageCommand
{
    override val ids: List<String>
        get() = listOf("test")
    override val helpText: String
        get() = "hilfe!"
    override val allowedArgumentCombinations: List<ArgumentCombination>
        get() = listOf(
            ArgumentCombination.empty("hilfe!") {
                val profile = UserRegistry.getGameProfile("Runebreaker") ?: return@empty false




                return@empty true
            }
        )
}