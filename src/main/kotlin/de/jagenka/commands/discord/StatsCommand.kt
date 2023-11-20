@file:Suppress("UNCHECKED_CAST")

package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.DiscordHandler
import de.jagenka.StatDataException
import de.jagenka.UserRegistry
import de.jagenka.Util.subListUntilOrEnd
import de.jagenka.commands.DiscordCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.argument
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal
import de.jagenka.commands.discord.MessageCommandSource.Companion.redirect
import de.jagenka.stats.StatData
import de.jagenka.stats.StatTypeArgument
import de.jagenka.stats.StatUtil
import net.minecraft.stat.StatType

object StatsCommand : DiscordCommand
{
    fun getReplyForAll(statType: StatType<Any>, id: String, limit: Int = 25): String
    {
        return try
        {
            val dataWithRanks = StatUtil.getStatDataWithRanks(statType, id)

            dataWithRanks
                .filter { it.second.value != 0 }
                .subListUntilOrEnd(limit)
                .joinToString(separator = System.lineSeparator()) {
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
    fun getReplyForSome(playerNames: Collection<String>, statType: StatType<Any>, id: String, limit: Int = 25): String
    {
        return try
        {
            val dataWithRanks = StatUtil.getStatDataWithRanks(statType, id)

            dataWithRanks
                .filter { playerNames.map { it.lowercase() }.contains(it.second.playerName.lowercase()) }
                .subListUntilOrEnd(limit)
                .joinToString(separator = System.lineSeparator()) {
                    format(rank = it.first, data = it.second)
                }
        } catch (e: StatDataException)
        {
            e.type.response
        }
    }

    private fun format(rank: Int, data: StatData) = "$rank. ${data.playerName.padEnd(17, ' ')} ${data.stat.format(data.value)}" // max length of player name is 16 character

    override val shortHelpText: String
        get() = "list players' stats"
    override val longHelpText: String
        get() = "query Minecraft stats of all or only some players. see https://github.com/Jagenka/DisKordel/blob/master/manual/queryable_stats.md for help."

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        val commandNode = dispatcher.register(literal("stat")
            .then(argument("statType", StatTypeArgument())
                .then(argument<String>("stat_identifier", StringArgumentType.word())
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
                    .then(
                        argument<Int>("topN", IntegerArgumentType.integer(1))
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
                    .then(argument<String>("partOfPlayerName", StringArgumentType.word())
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
                        .then(
                            argument<Int>("topN", IntegerArgumentType.integer(1))
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
                    )))
        )

        val alias = dispatcher.register(redirect("stats", commandNode))

        Registry.registerShortHelpText(shortHelpText, commandNode, alias)
        Registry.registerLongHelpText(longHelpText, commandNode, alias)
    }
}
