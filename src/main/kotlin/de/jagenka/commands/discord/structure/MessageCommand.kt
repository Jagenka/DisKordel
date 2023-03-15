package de.jagenka.commands.discord.structure

import dev.kord.core.event.message.MessageCreateEvent

interface MessageCommand : Comparable<MessageCommand>
{
    /**
     * This represents the literals by which the command is identified
     */
    val ids: List<String>

    /**
     * This represents, if the command needs to be executed in a channel marked NSFW.
     */
    val needsNSFW: Boolean
        get() = false

    /**
     * Short help text shown in command overview
     */
    val helpText: String

    /**
     * This method will be called immediately after registering it with a Registry.
     */
    fun prepare(registry: Registry) = Unit

    val allowedArgumentCombinations: List<ArgumentCombination>

    suspend fun run(event: MessageCreateEvent, args: List<String>): Boolean
    {
        allowedArgumentCombinations.sorted().forEach { combination ->
            if (combination.fitsTo(args.drop(1)))
            {
                if (combination.needsAdmin && Registry.isSenderAdmin.invoke(event) != true)
                {
                    Registry.needsAdminResponse.invoke(event)
                    return false
                }
                return combination.run(event, args)
            }
        }

        return false
    }

    override fun compareTo(other: MessageCommand): Int
    {
        return (ids.firstOrNull() ?: "").compareTo(other.ids.firstOrNull() ?: "")
    }
}