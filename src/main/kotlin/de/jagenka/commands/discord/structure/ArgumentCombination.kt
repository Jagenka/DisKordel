package de.jagenka.commands.discord.structure

import dev.kord.core.event.message.MessageCreateEvent

class ArgumentCombination(
    internal val arguments: List<Argument<*>>,
    internal val helpText: String,
    internal val needsAdmin: Boolean = false,
    private val executes: suspend (event: MessageCreateEvent, arguments: List<Pair<Argument<*>, String>>) -> Boolean // TODO: move List<Pair... to class
) : Comparable<ArgumentCombination>
{
    constructor(
        argument: Argument<*>,
        helpText: String,
        needsAdmin: Boolean = false,
        executes: suspend (event: MessageCreateEvent, arguments: List<Pair<Argument<*>, String>>) -> Boolean
    ) : this(listOf(argument), helpText, needsAdmin, executes)

    companion object
    {
        fun empty(helpText: String, needsAdmin: Boolean = false, executes: suspend (event: MessageCreateEvent) -> Boolean) =
            ArgumentCombination(emptyList(), helpText, needsAdmin) { event, _ ->
                executes(event)
            }

        fun List<Pair<Argument<*>, String>>.findInput(id: String): String
        {
            return this.firstOrNull { it.first.find(id) }?.second ?: ""
        }
    }

    internal fun fitsTo(args: List<String>): Boolean
    {
        if (args.size != arguments.size) return false

        arguments.forEachIndexed { index, argument ->
            if (!argument.isOfType(args[index])) return false
        }

        return true
    }

    internal suspend fun run(event: MessageCreateEvent, args: List<String>): Boolean
    {
        return executes.invoke(event, arguments zip args.drop(1))
    }

    private val rank: Int
        get()
        {
            return -arguments.count { it is LiteralArgument }
        }

    override fun compareTo(other: ArgumentCombination): Int
    {
        return rank.compareTo(other.rank)
    }
}