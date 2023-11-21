@file:Suppress("UNCHECKED_CAST")

package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.DiscordHandler
import de.jagenka.UserRegistry
import de.jagenka.commands.DiscordCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.argument
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal
import de.jagenka.commands.discord.MessageCommandSource.Companion.redirect
import de.jagenka.stats.StatTypeArgument
import de.jagenka.stats.StatUtil
import net.minecraft.stat.StatType

object StatsCommand : DiscordCommand
{
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
                            text = StatUtil.getStatReply(
                                statType = it.getArgument("statType", StatType::class.java) as StatType<Any>,
                                id = it.getArgument("stat_identifier", String::class.java),
                                queryType = StatUtil.StatQueryType.DEFAULT
                            ),
                            silent = true
                        )
                        0
                    }
                    .then(
                        argument<Int>("topN", IntegerArgumentType.integer(1))
                            .executes {
                                DiscordHandler.sendCodeBlock(
                                    text = StatUtil.getStatReply(
                                        statType = it.getArgument("statType", StatType::class.java) as StatType<Any>,
                                        id = it.getArgument("stat_identifier", String::class.java),
                                        queryType = StatUtil.StatQueryType.DEFAULT,
                                        limit = it.getArgument("topN", Int::class.java)
                                    ),
                                    silent = true
                                )
                                0
                            }
                    )
                    .then(argument<String>("partOfPlayerName", StringArgumentType.word())
                        .executes {
                            DiscordHandler.sendCodeBlock(
                                text = StatUtil.getStatReply(
                                    statType = it.getArgument("statType", StatType::class.java) as StatType<Any>,
                                    id = it.getArgument("stat_identifier", String::class.java),
                                    queryType = StatUtil.StatQueryType.DEFAULT,
                                    nameFilter = UserRegistry.findMinecraftProfiles(it.getArgument("partOfPlayerName", String::class.java)).map { it.name }
                                ),
                                silent = true
                            )
                            0
                        }
                        .then(
                            argument<Int>("topN", IntegerArgumentType.integer(1))
                                .executes {
                                    DiscordHandler.sendCodeBlock(
                                        text = StatUtil.getStatReply(
                                            statType = it.getArgument("statType", StatType::class.java) as StatType<Any>,
                                            id = it.getArgument("stat_identifier", String::class.java),
                                            queryType = StatUtil.StatQueryType.DEFAULT,
                                            nameFilter = UserRegistry.findMinecraftProfiles(it.getArgument("partOfPlayerName", String::class.java)).map { it.name },
                                            limit = it.getArgument("topN", Int::class.java)
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

        val alias = dispatcher.register(redirect("stats", commandNode))

        Registry.registerShortHelpText(shortHelpText, commandNode, alias)
        Registry.registerLongHelpText(longHelpText, commandNode, alias)
    }
}
