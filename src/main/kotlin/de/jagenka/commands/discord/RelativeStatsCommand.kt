package de.jagenka.commands.discord

import com.mojang.authlib.GameProfile
import de.jagenka.DiscordHandler
import de.jagenka.PlayerStatManager
import de.jagenka.UserRegistry
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
        try
        {
            val stat = statType.getOrCreateStat(statType.registry.get(Identifier(id)))
            val playtimeStat = Stats.CUSTOM.getOrCreateStat(Stats.CUSTOM.registry.get(Identifier("play_time")))

            return collection
                .mapNotNull {
                    it.name to ((PlayerStatManager.getStatHandlerForPlayer(it.name)?.getStat(stat) ?: return@mapNotNull null).toDouble()
                            / ((PlayerStatManager.getStatHandlerForPlayer(it.name)?.getStat(playtimeStat)
                        ?: return@mapNotNull null) / 72000.0)) // converts ticks to hours 20*60*60
                }
                .sortedByDescending { it.second }
                .filterNot { it.second == 0.0 }
                .joinToString(prefix = "```", separator = "\n", postfix = "```") { format(it.first, stat, it.second) }
                .replace("``````", "")
                .ifBlank { "Nothing found!" }
        } catch (_: Exception)
        {
            return "Nothing found!"
        }
    }

    private fun format(playerName: String, stat: Stat<*>, value: Double): String
    {
        var result = "${"$playerName:".padEnd(17, ' ')} " // max length of player name is 16 characters

        result +=
            if (stat.formatter == StatFormatter.TIME)
            {
                "${(value / 720.0).trimDecimals(2)}%" // converts ticks to hours but then to percent (value*100)/(20*60*60)
            } else if (stat.formatter == StatFormatter.DISTANCE)
            {
                "${(value / 100_000.0).trimDecimals(2)} km/h" // converts cm to km value/(100*1000)
            } else
            {
                "${value.trimDecimals(2)}/h"
            }

        return result
    }
}