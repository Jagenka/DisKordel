@file:Suppress("UNCHECKED_CAST")

package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.DiscordHandler
import de.jagenka.StatDataException
import de.jagenka.UserRegistry
import de.jagenka.Util
import de.jagenka.Util.trimDecimals
import de.jagenka.commands.DiscordCommand
import de.jagenka.stats.RStatData
import de.jagenka.stats.StatTypeArgument
import de.jagenka.stats.StatUtil
import net.minecraft.stat.Stat
import net.minecraft.stat.StatFormatter
import net.minecraft.stat.StatType

object RelativeStatsCommand : DiscordCommand
{
    fun getReplyForAll(statType: StatType<Any>, id: String): String
    {
        return try
        {
            val dataWithRanks = StatUtil.getStatDataWithRanks(statType, id)
            val playtimeData = StatUtil.getStatDataList()

            dataWithRanks.joinToString(separator = System.lineSeparator()) {
                format(rank = it.first, data = it.second)
            }
        } catch (e: StatDataException)
        {
            e.type.response
        }
    }

    /**
     * @param playerNames list of playerNames to filter. capitalization is ignored
     */
    fun getReplyForSome(playerNames: Collection<String>, statType: StatType<Any>, id: String): String
    {
        return try
        {
            val dataWithRanks = StatUtil.getStatDataWithRanks(statType, id)

            dataWithRanks
                .filter { playerNames.map { it.lowercase() }.contains(it.second.playerName.lowercase()) }
                .joinToString(separator = System.lineSeparator()) {
                    format(rank = it.first, data = it.second)
                }
        } catch (e: StatDataException)
        {
            e.type.response
        }
    }

    private fun format(data: RStatData, stat: Stat<*>): String
    {
        var result = "${"${data.playerName}:".padEnd(17, ' ')} " // max length of player name is 16 characters

        result +=
            when (stat.formatter)
            {
                StatFormatter.TIME ->
                {
                    "${(data.relStat / 720.0).trimDecimals(2)}%" // converts tick stats to hours but then to percent ((value*100)/(20*60*60))
                }

                StatFormatter.DISTANCE ->
                {
                    "${(data.relStat / 100_000.0).trimDecimals(2)} km/h" // converts cm stats to km (value/(100*1000))
                }

                else ->
                {
                    "${data.relStat.trimDecimals(2)}/h"
                }
            }

        result = result.padEnd(32, ' ') // 17 from above plus 15 (should be enough spacing)

        result += " (${stat.format(data.stat)} in ${Util.ticksToPrettyString(data.playtime)})"

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
                            text = getRelativeReplyForAll(
                                it.getArgument("statType", StatType::class.java) as StatType<Any>,
                                it.getArgument("stat_identifier", String::class.java)
                            ),
                            silent = true
                        )
                        0
                    }
                    .then(
                        MessageCommandSource.argument<String>("partOfPlayerName", StringArgumentType.word())
                            .executes {
                                val partOfPlayerName = it.getArgument("partOfPlayerName", String::class.java)
                                val statType = it.getArgument("statType", StatType::class.java) as StatType<Any>
                                val statIdentifier = it.getArgument("stat_identifier", String::class.java)
                                DiscordHandler.sendCodeBlock(
                                    text = getRelativeReplyForSome(UserRegistry.findMinecraftProfiles(partOfPlayerName), statType, statIdentifier),
                                    silent = true
                                )
                                0
                            }
                    )))
        )

        val alias = dispatcher.register(MessageCommandSource.redirect("rstats", commandNode))

        Registry.registerShortHelpText(shortHelpText, commandNode, alias)
        Registry.registerLongHelpText(longHelpText, commandNode, alias)
    }
}

