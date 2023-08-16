package de.jagenka.commands.discord

import com.mojang.authlib.GameProfile
import de.jagenka.DiscordHandler
import de.jagenka.PlayerStatManager
import de.jagenka.UserRegistry
import de.jagenka.Util
import de.jagenka.Util.trimDecimals
import de.jagenka.commands.discord.structure.Argument
import de.jagenka.commands.discord.structure.ArgumentCombination
import de.jagenka.commands.discord.structure.MessageCommand
import net.minecraft.stat.Stat
import net.minecraft.stat.StatFormatter
import net.minecraft.stat.StatType
import net.minecraft.stat.Stats
import net.minecraft.util.Identifier

object RelativeStatsCommand : MessageCommand
{
    override val ids: List<String>
        get() = listOf("rstat", "rstats")
    override val helpText: String
        get() = "display non-zero stats in relation to the player's playtime"
    override val allowedArgumentCombinations: List<ArgumentCombination>
        get() = listOf(
            ArgumentCombination(listOf(StatArgument(), Argument.string("stat")), "Get stat for all players.") { event, arguments ->
                val (argType, argText) = arguments[0]
                DiscordHandler.sendMessage(
                    getRelativeReplyForAll(
                        (argType as StatArgument).convertToType(argText) ?: return@ArgumentCombination false,
                        arguments[1].second
                    )
                )
                true
            },
            ArgumentCombination(listOf(StatArgument(), Argument.string("stat"), Argument.string("partOfName")), "Get stat for some players.") { event, arguments ->
                val (_, playerName) = arguments[2]
                val (argType, argText) = arguments[0]
                DiscordHandler.sendMessage(
                    getRelativeReplyForSome(
                        UserRegistry.findMinecraftProfiles(playerName),
                        (argType as StatArgument).convertToType(argText) ?: return@ArgumentCombination false,
                        arguments[1].second
                    )
                )
                true
            },
        )

    private fun getRelativeReplyForAll(statType: StatType<Any>, id: String): String
    {
        return getRelativeReplyForSome(UserRegistry.getMinecraftProfiles(), statType, id)
    }

    private fun getRelativeReplyForSome(collection: Collection<GameProfile>, statType: StatType<Any>, id: String): String
    {
        val defaultResponse = "Nothing found!"

        try
        {
            val identifier = Identifier(id)
            val registry = statType.registry
            val key = registry.get(identifier) ?: return defaultResponse
            if (registry.getId(key) != identifier) return defaultResponse
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
                .joinToString(prefix = "```", separator = "\n", postfix = "```") { format(it, stat) }
                .replace("``````", "")
                .ifBlank { defaultResponse }
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

        result += " (${data.stat} in ${Util.ticksToPrettyString(data.playtime)})"

        return result
    }

    data class RStatData(val playerName: String, val stat: Int, val playtime: Int)
    {
        val relStat: Double
            get() = (stat.toDouble() * 72_000.0) / playtime.toDouble() // converts ticks to hours (20*60*60)
    }
}