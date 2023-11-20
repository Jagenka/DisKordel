@file:Suppress("UNCHECKED_CAST")

package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.DiscordHandler
import de.jagenka.StatDataException
import de.jagenka.UserRegistry
import de.jagenka.Util
import de.jagenka.Util.subListUntilOrEnd
import de.jagenka.Util.trimDecimals
import de.jagenka.commands.DiscordCommand
import de.jagenka.stats.StatData
import de.jagenka.stats.StatTypeArgument
import de.jagenka.stats.StatUtil
import de.jagenka.stats.StatUtil.ticks
import net.minecraft.stat.StatFormatter
import net.minecraft.stat.StatType
import kotlin.math.max
import kotlin.time.Duration.Companion.hours

object PlaytimePerStatCommand : DiscordCommand
{
    /**
     * @param limit top n list entries to show
     */
    fun getReplyForAll(statType: StatType<Any>, id: String, limit: Int = 25): String
    {
        return try
        {
            val dataWithRanks = StatUtil.getInverseRelativeStatDataWithRank(statType, id)

            dataWithRanks
                .subListUntilOrEnd(limit)
                .joinToString(separator = System.lineSeparator()) { (rank, statData, playtimeData) ->
                    format(rank, statData, playtimeData)
                }
        } catch (e: StatDataException)
        {
            e.type.response
        }
    }

    /**
     * @param playerNames list of playerNames to filter. capitalization is ignored
     * @param limit top n list entries to show, null means no limit
     */
    fun getReplyForSome(playerNames: Collection<String>, statType: StatType<Any>, id: String, limit: Int = 25): String
    {
        return try
        {
            val dataWithRanks = StatUtil.getInverseRelativeStatDataWithRank(statType, id)

            dataWithRanks
                .filter { playerNames.map { it.lowercase() }.contains(it.second.playerName.lowercase()) }
                .subListUntilOrEnd(limit)
                .joinToString(separator = System.lineSeparator()) { (rank, statData, playtimeData) ->
                    format(rank, statData, playtimeData)
                }
        } catch (e: StatDataException)
        {
            e.type.response
        }
    }

    private fun format(rank: Int, statData: StatData, playtimeData: StatData): String
    {
        val inverseRelStat = StatUtil.getInverseRelStat(statData.value, playtimeData.value) // hours per stat

        var result = "${rank.toString().padStart(2, ' ')}. ${statData.playerName.padEnd(17, ' ')} " // max length of player name is 16 characters

        result +=
            when (statData.stat.formatter)
            {
                StatFormatter.TIME ->
                {
                    "${((playtimeData.value.toDouble() * 100) / max(1.0, statData.value.toDouble())).trimDecimals(2)}%"
                }

                StatFormatter.DISTANCE ->
                {
                    if (statData.value == 0)
                    {
                        playtimeData.value.ticks.toComponents { hours, minutes, seconds, _ ->
                            "${hours}h ${minutes}min ${seconds}s /0km"
                        }
                    } else
                    {
                        (inverseRelStat * 100000).hours.toComponents { hours, minutes, seconds, _ -> // h/cm to h/km
                            "${hours}h ${minutes}min ${seconds}s /km"
                        }
                    }
                }

                else ->
                {
                    inverseRelStat.hours.toComponents { hours, minutes, seconds, _ ->
                        "${hours}h ${minutes}min ${seconds}s"
                    }
                }
            }

        result = result.padEnd(32, ' ') // 17 from above plus 15 (should be enough spacing)

        result += " (${Util.ticksToPrettyString(playtimeData.value)} for ${statData.stat.format(statData.value)})"

        return result
    }

    override val shortHelpText: String
        get() = "list players' playtime in relation to a stat"
    override val longHelpText: String
        get() = "query playtime of all or only some players and relate them to some Minecraft stat. see https://github.com/Jagenka/DisKordel/blob/master/manual/queryable_stats.md for help."

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        val commandNode = dispatcher.register(MessageCommandSource.literal("pstat")
            .then(MessageCommandSource.argument("statType", StatTypeArgument())
                .then(MessageCommandSource.argument<String>("stat_identifier", StringArgumentType.word())
                    .executes {
                        DiscordHandler.sendCodeBlock(
                            text = getReplyForAll(
                                it.getArgument("statType", StatType::class.java) as StatType<Any>,
                                it.getArgument("stat_identifier", String::class.java)
                            ),
                            silent = true
                        )
                        0
                    }
                    .then(MessageCommandSource.argument<Int>("topN", IntegerArgumentType.integer(1))
                        .executes {
                            val topN = it.getArgument("topN", Int::class.java)
                            val statType = it.getArgument("statType", StatType::class.java) as StatType<Any>
                            val statIdentifier = it.getArgument("stat_identifier", String::class.java)
                            DiscordHandler.sendCodeBlock(
                                text = getReplyForAll(statType, statIdentifier, limit = topN),
                                silent = true
                            )
                            0
                        }
                    )
                    .then(MessageCommandSource.argument<String>("partOfPlayerName", StringArgumentType.word())
                        .executes {
                            val partOfPlayerName = it.getArgument("partOfPlayerName", String::class.java)
                            val statType = it.getArgument("statType", StatType::class.java) as StatType<Any>
                            val statIdentifier = it.getArgument("stat_identifier", String::class.java)
                            DiscordHandler.sendCodeBlock(
                                text = getReplyForSome(UserRegistry.findMinecraftProfiles(partOfPlayerName).map { it.name }, statType, statIdentifier),
                                silent = true
                            )
                            0
                        }
                        .then(MessageCommandSource.argument<Int>("topN", IntegerArgumentType.integer(1))
                            .executes {
                                val partOfPlayerName = it.getArgument("partOfPlayerName", String::class.java)
                                val topN = it.getArgument("topN", Int::class.java)
                                val statType = it.getArgument("statType", StatType::class.java) as StatType<Any>
                                val statIdentifier = it.getArgument("stat_identifier", String::class.java)
                                DiscordHandler.sendCodeBlock(
                                    text = getReplyForSome(
                                        UserRegistry.findMinecraftProfiles(partOfPlayerName).map { it.name },
                                        statType,
                                        statIdentifier,
                                        limit = topN
                                    ),
                                    silent = true
                                )
                                0
                            }
                        )
                    )
                )
            )
        )

        val alias = dispatcher.register(MessageCommandSource.redirect("pstats", commandNode))

        Registry.registerShortHelpText(shortHelpText, commandNode, alias)
        Registry.registerLongHelpText(longHelpText, commandNode, alias)
    }
}

