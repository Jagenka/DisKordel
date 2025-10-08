package de.jagenka.commands.discord

import de.jagenka.StatDataException
import de.jagenka.UserRegistry
import de.jagenka.Util.trimDecimals
import de.jagenka.commands.DiskordelSlashCommand
import de.jagenka.stats.StatQueryType
import de.jagenka.stats.StatRequest
import de.jagenka.stats.StatUtil
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.*
import net.minecraft.stat.StatType
import net.minecraft.stat.Stats

/**
 * Displays the average speed of all players.
 * Could get moved to StatRequest?
 */
object SpeedCommand : DiskordelSlashCommand
{
    override val name: String
        get() = "speed"
    override val description: String
        get() = "Displays all player's average speed, adding up all _one_cm stats, divided by playtime."

    val distanceStatIds = listOf(
        "climb_one_cm",
        "crouch_one_cm",
        "fall_one_cm",
        "fly_one_cm",
        "sprint_one_cm",
        "swim_one_cm",
        "walk_one_cm",
        "walk_on_water_one_cm",
        "walk_under_water_one_cm",
        "boat_one_cm",
        "aviate_one_cm",
        "horse_one_cm",
        "minecart_one_cm",
        "pig_one_cm",
        "strider_one_cm"
    )

    override suspend fun build(builder: RootInputChatBuilder)
    {
        with(builder)
        {
            subCommand("get", "Get player's average speed.")
            {
                integer("top_n", "How many entries to display starting from 1st.")
                {
                    required = false
                    minValue = 1
                }
                string("part_of_name", "Part of a player's name.")
                { required = false }
                boolean("ascending", "If the result should be sorted ascending, rather than descending.")
                { required = false }
            }

            subCommand("compare", "Compare stats between yourself and another player, or two other players.")
            {
                string("player", "Part of a player's name.")
                { required = true }
                string("player2", "Part of a player's name.")
                { required = false }
                boolean("ascending", "If the result should be sorted ascending, rather than descending.")
                { required = false }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        with(event)
        {
            val subcommands = interaction.command.data.options.value?.map { it.name } ?: emptyList()

            when (subcommands.firstOrNull())
            {
                "compare" ->
                {
                    interaction.respondEphemeral {
                        content = "Not yet implemented."
                    }
                }

                "get" ->
                {
                    val response = interaction.deferEphemeralResponse()

                    val ascending = interaction.command.booleans["ascending"]

                    val partOfName = interaction.command.strings["part_of_name"]
                    val profileFilter = if (partOfName != null) UserRegistry.findMinecraftProfiles(partOfName) else emptyList()

                    val topN = interaction.command.integers["top_n"]

                    try
                    {
                        /**
                         * contains (playerName: String, sumInCm: Int, playtime: Int) entries of all queried players
                         */
                        val statEntriesSummed = distanceStatIds.map { statId ->
                            StatRequest(
                                statType = Stats.CUSTOM as StatType<Any>,
                                id = statId,
                                queryType = StatQueryType.DEFAULT,
                                ascending = ascending ?: false,
                                profileFilter = profileFilter,
                                topN = topN?.toInt(),
                                invoker = UserRegistry.findUser(interaction.user.id)?.minecraft?.uuid?.let { UserRegistry.getGameProfile(it, true) },
                            ).requestResult
                        }
                            .flatten()
                            .groupBy { it.playerName }
                            .map { (playerName, statEntries) ->
                                val sumInCm = statEntries.sumOf { it.stat.value }
                                return@map Triple(playerName, sumInCm, statEntries.first().playtime.value)
                            }

                        response.respond { // TODO: make pretty output
                            content = statEntriesSummed.joinToString(separator = System.lineSeparator()) { (name, distance, playtime) ->
                                val relStat = StatUtil.getRelStat(distance, playtime)
                                "${name}: $relStat cm/h = ${(relStat / 100_000.0).trimDecimals(3)} km/h" // converts cm stats to km (value/(100*1000))
                            }
                        }

                    } catch (exception: StatDataException)
                    {
                        response.respond { content = exception.type.response }
                    }
                }
            }
        }
    }
}