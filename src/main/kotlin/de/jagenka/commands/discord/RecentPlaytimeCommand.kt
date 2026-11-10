package de.jagenka.commands.discord

import de.jagenka.UserRegistry
import de.jagenka.Util.ticksToPrettyString
import de.jagenka.commands.DiskordelSlashCommand
import de.jagenka.stats.PlaytimeHistory
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.RootInputChatBuilder
import dev.kord.rest.builder.interaction.boolean
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.string

private const val DEFAULT_DAYS = 7

object RecentPlaytimeCommand : DiskordelSlashCommand
{
    override val name: String
        get() = "recent"
    override val description: String
        get() = "List players by how much they've played in the last N days (default $DEFAULT_DAYS)."

    override suspend fun build(builder: RootInputChatBuilder)
    {
        with(builder)
        {
            integer("days", "How many days to look back. Defaults to $DEFAULT_DAYS.")
            {
                required = false
                minValue = 1
            }
            string("part_of_name", "Part of a player's name.")
            { required = false }
            integer("limit", "How many entries to display.")
            {
                required = false
                minValue = 1
            }
            boolean("ascending", "If the result should be sorted ascending, rather than descending.")
            { required = false }
        }
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        with(event)
        {
            val response = interaction.deferEphemeralResponse()

            val days = interaction.command.integers["days"]?.toInt() ?: DEFAULT_DAYS
            val partOfName = interaction.command.strings["part_of_name"]
            val limit = interaction.command.integers["limit"]?.toInt()
            val ascending = interaction.command.booleans["ascending"] ?: false

            val profiles = if (partOfName != null) UserRegistry.findMinecraftProfiles(partOfName) else UserRegistry.getMinecraftProfiles()

            var rows = profiles
                .mapNotNull { profile -> PlaytimeHistory.recentPlaytimeTicks(profile.name, days)?.let { profile.name to it } }
                .sortedWith(if (ascending) compareBy { it.second } else compareByDescending { it.second })

            if (limit != null) rows = rows.take(limit)

            if (rows.isEmpty())
            {
                response.respond { content = "No players found." }
                return
            }

            val reply = rows.mapIndexed { index, (playerName, ticks) ->
                "${(index + 1).toString().padStart(2, ' ')}. ${playerName.padEnd(17, ' ')} ${ticksToPrettyString(ticks)}"
            }.joinToString(separator = "\n", prefix = "Playtime in the last $days day(s):\n")

            response.respond { content = reply }
        }
    }
}
