package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.commands.discord.structure.Argument
import de.jagenka.commands.discord.structure.ArgumentCombination
import de.jagenka.commands.discord.structure.ArgumentCombination.Companion.findInput
import de.jagenka.commands.discord.structure.MessageCommand
import de.jagenka.commands.discord.structure.Registry

object HelpMessageCommand : MessageCommand
{
    override val ids: List<String>
        get() = listOf("help", "?")
    override val needsNSFW: Boolean
        get() = false
    override val helpText: String
        get() = "Get help for commands."

    override fun prepare(registry: Registry) = Unit

    override val allowedArgumentCombinations: List<ArgumentCombination>
        get()
        {
            Registry.let { registry ->
                return listOf(
                    ArgumentCombination(emptyList(), "Displays this help text.") { event, _ ->
                        DiscordHandler.sendMessage(registry.getShortHelpTexts(event).joinToString(separator = System.lineSeparator()))
                        true
                    },
                    ArgumentCombination(listOf(Argument.string("command")), "Get help for a specific command.") { event, arguments ->
                        DiscordHandler.sendMessage(
                            registry.getHelpTextsForCommand(arguments.findInput("command"), event).joinToString(separator = System.lineSeparator())
                        )
                        true
                    }
                )
            }
        }
}