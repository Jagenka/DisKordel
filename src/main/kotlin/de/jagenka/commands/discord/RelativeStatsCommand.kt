@file:Suppress("UNCHECKED_CAST")

package de.jagenka.commands.discord

import com.mojang.authlib.GameProfile
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.DiscordHandler
import de.jagenka.PlayerStatManager
import de.jagenka.UserRegistry
import de.jagenka.Util
import de.jagenka.Util.trimDecimals
import de.jagenka.commands.DiscordCommand
import net.minecraft.stat.Stat
import net.minecraft.stat.StatFormatter
import net.minecraft.stat.StatType
import net.minecraft.stat.Stats
import net.minecraft.util.Identifier

object RelativeStatsCommand : DiscordCommand
{
    private fun getRelativeReplyForAll(statType: StatType<Any>, id: String): String
    {
        return getRelativeReplyForSome(UserRegistry.getMinecraftProfiles(), statType, id)
    }

    private fun getRelativeReplyForSome(collection: Collection<GameProfile>, statType: StatType<Any>, id: String): String
    {
        val defaultResponse = "Nothing found."
        val invalidId = "Invalid stat identifier."
        val noNonZeroValuesFound = "Only zero(es) found!"

        try
        {
            val identifier = Identifier(id)
            val registry = statType.registry
            val key = registry.get(identifier) ?: return invalidId
            if (registry.getId(key) != identifier) return invalidId
            val stat = statType.getOrCreateStat(key)
            val playtimeStat = Stats.CUSTOM.getOrCreateStat(Stats.CUSTOM.registry.get(Identifier("play_time")))

            return collection
                .mapNotNull {
                    RStatData(
                        it.name,
                        (PlayerStatManager.getStatHandlerForPlayer(it.name)?.getStat(stat) ?: return@mapNotNull null),
                        (PlayerStatManager.getStatHandlerForPlayer(it.name)?.getStat(playtimeStat) ?: return@mapNotNull null)
                    )
                }
                .sortedByDescending { it.relStat }
                .filter { it.relStat > 0.0 }
                .joinToString(separator = "\n") { format(it, stat) }
                .replace("``````", "")
                .ifBlank { noNonZeroValuesFound }
        } catch (_: Exception)
        {
            return defaultResponse
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

        dispatcher.register(MessageCommandSource.redirect("rstats", commandNode))
    }
}

data class RStatData(val playerName: String, val stat: Int, val playtime: Int)
{
    val relStat: Double
        get() = (if (playtime == 0) 0.0 else (stat.toDouble() * 72_000.0)) / playtime.toDouble() // converts ticks to hours (20*60*60)
}