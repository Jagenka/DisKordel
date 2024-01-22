@file:Suppress("UNCHECKED_CAST")

package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import de.jagenka.DiscordHandler
import de.jagenka.UserRegistry
import de.jagenka.commands.DiscordCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.argument
import de.jagenka.stats.StatTypeArgument
import de.jagenka.stats.StatUtil
import net.minecraft.stat.StatType

object CompareStatsCommand : DiscordCommand
{
    private fun sendComparison(type: StatType<Any>, id: String, player1: String?, player2: String?)
    {
        if (player1 == null)
        {
            DiscordHandler.sendMessage("Error finding player1.", silent = true)
            return
        }

        if (player2 == null)
        {
            DiscordHandler.sendMessage("Error finding player2.", silent = true)
            return
        }

        val playerNames = listOf(player1, player2)

        DiscordHandler.sendCodeBlock(
            text = StatUtil.getStatReply(
                statType = type,
                id = id,
                queryType = StatUtil.StatQueryType.COMPARE,
                nameFilter = playerNames
            ),
            silent = true
        )
    }

    override val shortHelpText: String
        get() = "compare two player's stats"
    override val longHelpText: String
        get() = "compare your own or someone else's stats to another player. stat criteria are the same as with the normal stat command."

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        val commandNode = dispatcher.register(LiteralArgumentBuilder.literal<MessageCommandSource?>("cstat")
            .then(argument("statType", StatTypeArgument())
                .then(argument<String>("stat_identifier", StringArgumentType.word())
                    .then(argument<String>("player1", StringArgumentType.word())
                        .executes {
                            val player1Input = it.getArgument("player1", String::class.java)
                            val player1 = UserRegistry.findMostLikelyMinecraftName(player1Input)
                            val player2 = UserRegistry.findUser(it.source.author?.id)?.minecraft?.name
                            sendComparison(
                                it.getArgument("statType", StatType::class.java) as StatType<Any>,
                                it.getArgument("stat_identifier", String::class.java),
                                player1,
                                player2
                            )
                            0
                        }
                        .then(argument<String>("player2", StringArgumentType.word())
                            .executes {
                                val player1Input = it.getArgument("player1", String::class.java)
                                val player2Input = it.getArgument("player2", String::class.java)
                                val player1 = UserRegistry.findMostLikelyMinecraftName(player1Input)
                                val player2 = UserRegistry.findMostLikelyMinecraftName(player2Input)
                                sendComparison(
                                    it.getArgument("statType", StatType::class.java) as StatType<Any>,
                                    it.getArgument("stat_identifier", String::class.java),
                                    player1,
                                    player2
                                )
                                0
                            }
                        )
                    )
                )
            )
        )

        val alias = dispatcher.register(MessageCommandSource.redirect("cstats", commandNode))

        Registry.registerShortHelpText(shortHelpText, commandNode, alias)
        Registry.registerLongHelpText(longHelpText, commandNode, alias)
    }
}