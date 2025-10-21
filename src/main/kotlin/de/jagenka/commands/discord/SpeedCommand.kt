package de.jagenka.commands.discord

import de.jagenka.StatDataException
import de.jagenka.UserRegistry
import de.jagenka.commands.DiskordelSlashCommand
import de.jagenka.stats.StatQueryType
import de.jagenka.stats.StatRequest
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
                    val response = interaction.deferEphemeralResponse()

                    val ascending = interaction.command.booleans["ascending"]
                    val player1 = UserRegistry.findMostLikelyMinecraftName(interaction.command.strings["player"]!!)
                    val player2Input = interaction.command.strings["player2"]
                    val player2 =
                        if (player2Input != null) UserRegistry.findMostLikelyMinecraftName(player2Input)
                        else UserRegistry.findUser(interaction.user.id)?.minecraft?.username
                    if (player2 == null)
                    {
                        response.respond { content = "error finding player2" }
                        return
                    }
                    val profileFilter = UserRegistry.findMinecraftProfiles(player1).union(UserRegistry.findMinecraftProfiles(player2))
                    if (profileFilter.isEmpty())
                    {
                        response.respond { content = "no players found." }
                        return
                    }

                    try
                    {
                        val statRequestResponse = StatRequest(
                            statType = Stats.CUSTOM as StatType<Any>,
                            id = "speed",
                            queryType = StatQueryType.STAT_PER_TIME,
                            ascending = ascending ?: false,
                            profileFilter = profileFilter,
                            topN = 2,
                            invoker = UserRegistry.findUser(interaction.user.id)?.minecraft?.uuid?.let { UserRegistry.getGameProfile(it, true) },
                        )

                        response.respond {
                            content = statRequestResponse.getReplyString()
                        }

                    } catch (exception: StatDataException)
                    {
                        response.respond { content = exception.type.response }
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
                        val statRequestResponse = StatRequest(
                            statType = Stats.CUSTOM as StatType<Any>,
                            id = "speed",
                            queryType = StatQueryType.STAT_PER_TIME,
                            ascending = ascending ?: false,
                            profileFilter = profileFilter,
                            topN = topN?.toInt(),
                            invoker = UserRegistry.findUser(interaction.user.id)?.minecraft?.uuid?.let { UserRegistry.getGameProfile(it, true) },
                        )

                        response.respond {
                            content = statRequestResponse.getReplyString()
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