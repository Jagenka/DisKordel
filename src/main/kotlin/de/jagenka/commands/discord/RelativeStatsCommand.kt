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
import net.minecraft.stat.StatFormatter
import net.minecraft.stat.StatType

object RelativeStatsCommand : DiscordCommand
{
    /**
     * @param limit top n list entries to show
     */
    fun getReplyForAll(statType: StatType<Any>, id: String, limit: Int = 25): String
    {
        return try
        {
            val dataWithRanks = StatUtil.getRelativeStatDataWithRank(statType, id)

            dataWithRanks
                .filter { it.second.value != 0 }
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
            val dataWithRanks = StatUtil.getRelativeStatDataWithRank(statType, id)

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
        val relStat = StatUtil.getRelStat(statData.value, playtimeData.value)

        var result = "${rank.toString().padStart(2, ' ')}. ${statData.playerName.padEnd(17, ' ')} " // max length of player name is 16 characters

        result +=
            when (statData.stat.formatter)
            {
                StatFormatter.TIME ->
                {
                    "${(relStat / 720.0).trimDecimals(2)}%" // converts tick stats to hours but then to percent ((value*100)/(20*60*60))
                }

                StatFormatter.DISTANCE ->
                {
                    "${(relStat / 100_000.0).trimDecimals(2)} km/h" // converts cm stats to km (value/(100*1000))
                }

                else ->
                {
                    "${relStat.trimDecimals(2)}/h"
                }
            }

        result = result.padEnd(32, ' ') // 17 from above plus 15 (should be enough spacing)

        result += " (${statData.stat.format(statData.value)} in ${Util.ticksToPrettyString(playtimeData.value)})"

        return result
    }

    override val shortHelpText: String
        get() = "list players' stats in relation to playtime"
    override val longHelpText: String
        get() = "query Minecraft stats of all or only some players and relate them to their playtime. see https://github.com/Jagenka/DisKordel/blob/master/manual/queryable_stats.md for help."

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        val commandNode = dispatcher.register(MessageCommandSource.literal("rstat")
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

        val alias = dispatcher.register(MessageCommandSource.redirect("rstats", commandNode))

        Registry.registerShortHelpText(shortHelpText, commandNode, alias)
        Registry.registerLongHelpText(longHelpText, commandNode, alias)
    }
}

