@file:Suppress("UNCHECKED_CAST")

package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.DiscordHandler
import de.jagenka.UserRegistry
import de.jagenka.commands.DiskordelTextCommand
import de.jagenka.stats.StatTypeArgument
import de.jagenka.stats.StatUtil
import net.minecraft.stat.StatType

object RelativeStatsCommand : DiskordelTextCommand
{
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
                        DiscordHandler.sendMessage(
                            text = StatUtil.getStatReply(
                                statType = it.getArgument("statType", StatType::class.java) as StatType<Any>,
                                id = it.getArgument("stat_identifier", String::class.java),
                                queryType = StatUtil.StatQueryType.STAT_PER_TIME,
                                topN = null
                            ),
                            silent = true
                        )
                        0
                    }
                    .then(
                        MessageCommandSource.argument<Int>("topN", IntegerArgumentType.integer(1))
                            .executes {
                                DiscordHandler.sendMessage(
                                    text = StatUtil.getStatReply(
                                        statType = it.getArgument("statType", StatType::class.java) as StatType<Any>,
                                        id = it.getArgument("stat_identifier", String::class.java),
                                        queryType = StatUtil.StatQueryType.STAT_PER_TIME,
                                        topN = it.getArgument("topN", Int::class.java)
                                    ),
                                    silent = true
                                )
                                0
                            }
                    )
                    .then(MessageCommandSource.argument<String>("partOfPlayerName", StringArgumentType.word())
                        .executes {
                            DiscordHandler.sendMessage(
                                text = StatUtil.getStatReply(
                                    statType = it.getArgument("statType", StatType::class.java) as StatType<Any>,
                                    id = it.getArgument("stat_identifier", String::class.java),
                                    queryType = StatUtil.StatQueryType.STAT_PER_TIME,
                                    nameFilter = UserRegistry.findMinecraftProfiles(it.getArgument("partOfPlayerName", String::class.java)),
                                    topN = null
                                ),
                                silent = true
                            )
                            0
                        }
                        .then(
                            MessageCommandSource.argument<Int>("topN", IntegerArgumentType.integer(1))
                                .executes {
                                    DiscordHandler.sendMessage(
                                        text = StatUtil.getStatReply(
                                            statType = it.getArgument("statType", StatType::class.java) as StatType<Any>,
                                            id = it.getArgument("stat_identifier", String::class.java),
                                            queryType = StatUtil.StatQueryType.STAT_PER_TIME,
                                            nameFilter = UserRegistry.findMinecraftProfiles(it.getArgument("partOfPlayerName", String::class.java)),
                                            topN = it.getArgument("topN", Int::class.java)
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

