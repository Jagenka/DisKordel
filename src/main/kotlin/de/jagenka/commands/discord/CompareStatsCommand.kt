@file:Suppress("UNCHECKED_CAST")

package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import de.jagenka.DiscordHandler
import de.jagenka.StatDataException
import de.jagenka.UserRegistry
import de.jagenka.commands.DiscordCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.argument
import de.jagenka.stats.StatData
import de.jagenka.stats.StatTypeArgument
import de.jagenka.stats.StatUtil
import net.minecraft.stat.StatType

object CompareStatsCommand : DiscordCommand
{
    fun sendComparison(type: StatType<Any>, id: String, player1: String, player2: String)
    {
        val playerNames = listOf(player1, player2)

        try
        {
            val dataWithRanks = StatUtil.getStatDataWithRanks(type, id)

            DiscordHandler.sendCodeBlock(
                text = dataWithRanks
                    .filter { playerNames.map { it.lowercase() }.contains(it.second.playerName.lowercase()) }
                    .joinToString(separator = System.lineSeparator()) {
                        format(it.first, it.second)
                    },
                silent = true
            )
        } catch (e: StatDataException)
        {
            DiscordHandler.sendMessage(e.type.response, silent = true)
        }
    }

    private fun format(rank: Int, data: StatData) =
        "${rank.toString().padStart(2, ' ')}. ${data.playerName.padEnd(17, ' ')} ${data.stat.format(data.value)}" // max length of player name is 16 character

    override val shortHelpText: String
        get() = "compare two player's stats"
    override val longHelpText: String
        get() = "compare your own or someone else's stats just like with stat command"

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        val commandNode = dispatcher.register(LiteralArgumentBuilder.literal<MessageCommandSource?>("cstat")
            .then(argument("statType", StatTypeArgument())
                .then(argument<String>("stat_identifier", StringArgumentType.word())
                    .then(argument<String>("player1", StringArgumentType.word())
                        .executes {
                            val player1Input = it.getArgument("player1", String::class.java)
                            val player1 = UserRegistry.findMostLikelyMinecraftName(player1Input)
                                ?: UserRegistry.findRegistered(player1Input).randomOrNull()?.minecraft?.name
                                ?: return@executes 1
                            val player2 = UserRegistry.findUser(it.source.author?.id)?.minecraft?.name ?: return@executes 1
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
                                    ?: UserRegistry.findRegistered(player1Input).randomOrNull()?.minecraft?.name
                                    ?: return@executes 1
                                val player2 = UserRegistry.findMostLikelyMinecraftName(player2Input)
                                    ?: UserRegistry.findRegistered(player2Input).randomOrNull()?.minecraft?.name
                                    ?: return@executes 1
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