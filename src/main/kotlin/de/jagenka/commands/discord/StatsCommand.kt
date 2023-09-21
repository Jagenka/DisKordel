package de.jagenka.commands.discord

import com.mojang.authlib.GameProfile
import de.jagenka.DiscordHandler
import de.jagenka.PlayerStatManager
import de.jagenka.UserRegistry
import de.jagenka.commands.discord.structure.Argument
import de.jagenka.commands.discord.structure.Argument.Companion.string
import de.jagenka.commands.discord.structure.ArgumentCombination
import de.jagenka.commands.discord.structure.MessageCommand
import net.minecraft.stat.Stat
import net.minecraft.stat.StatType
import net.minecraft.stat.Stats
import net.minecraft.util.Identifier
import java.util.*

object StatsCommand : MessageCommand
{
    override val ids: List<String>
        get() = listOf("stat", "stats")
    override val helpText: String
        get() = "Display non-zero stats for players."
    override val allowedArgumentCombinations: List<ArgumentCombination>
        get() = listOf(
            ArgumentCombination(listOf(StatArgument(), string("stat")), "Get stat for all players.") { event, arguments ->
                val (argType, argText) = arguments[0]
                DiscordHandler.sendCodeBlock(
                    text = getReplyForAll(
                        (argType as StatArgument).convertToType(argText) ?: return@ArgumentCombination false,
                        arguments[1].second
                    )
                )
                true
            },
            ArgumentCombination(listOf(StatArgument(), string("stat"), string("partOfName")), "Get stat for some players.") { event, arguments ->
                val (_, playerName) = arguments[2]
                val (argType, argText) = arguments[0]
                DiscordHandler.sendCodeBlock(
                    text = getReplyForSome(
                        UserRegistry.findMinecraftProfiles(playerName),
                        (argType as StatArgument).convertToType(argText) ?: return@ArgumentCombination false,
                        arguments[1].second
                    )
                )
                true
            },
        )

    private fun format(playerName: String, stat: Stat<*>, value: Int) = "${"$playerName:".padEnd(17, ' ')} ${stat.format(value)}" // max length of player name is 16 character

    private fun handle(playerName: String, statType: StatType<Any>, id: String): String?
    {
        return try
        {
            getReplyWithStat(playerName, statType.getOrCreateStat(statType.registry.get(Identifier(id))))
        } catch (_: Exception)
        {
            null
        }
    }

    fun getReplyForAll(statType: StatType<Any>, id: String): String
    {
        return getReplyForSome(UserRegistry.getMinecraftProfiles(), statType, id)
    }

    fun getReplyForSome(collection: Collection<GameProfile>, statType: StatType<Any>, id: String): String
    {
        val defaultResponse = "Nothing found!"

        try
        {
            val identifier = Identifier(id)
            val registry = statType.registry
            val key = registry.get(identifier) ?: return defaultResponse
            if (registry.getId(key) != identifier) return defaultResponse
            val stat = statType.getOrCreateStat(key)

            return collection
                .mapNotNull { it.name to (PlayerStatManager.getStatHandlerForPlayer(it.name)?.getStat(stat) ?: return@mapNotNull null) }
                .sortedByDescending { it.second }
                .filterNot { it.second == 0 }
                .joinToString(separator = "\n") { format(it.first, stat, it.second) }
                .replace("``````", "")
                .ifBlank { defaultResponse }
        } catch (_: Exception)
        {
            return defaultResponse
        }
    }

    private fun getReplyWithStat(playerName: String, stat: Stat<*>): String?
    {
        val statValue = PlayerStatManager.getStatHandlerForPlayer(playerName)?.getStat(stat)
        return statValue?.let { stat.format(it) }
    }

    private fun handle(uuid: UUID, statType: StatType<Any>, id: String): String?
    {
        return try
        {
            getReplyWithStat(uuid, statType.getOrCreateStat(statType.registry.get(Identifier(id))))
        } catch (_: Exception)
        {
            null
        }
    }

    private fun getReplyWithStat(uuid: UUID, stat: Stat<*>): String?
    {
        val statValue = PlayerStatManager.getStatHandlerForPlayer(uuid)?.getStat(stat)
        return statValue?.let { stat.format(it) }
    }
}

class StatArgument : Argument<StatType<Any>>
{
    private val literals = listOf("mined", "crafted", "used", "broken", "picked_up", "dropped", "killed", "killed_by", "custom")

    override val id: String
        get() = literals.joinToString("|")

    override fun isOfType(word: String): Boolean
    {
        return word in literals
    }

    override fun convertToType(word: String): StatType<Any>?
    {
        return convert(word)
    }

    companion object
    {
        fun convert(word: String): StatType<Any>?
        {
            try
            {
                return when (word)
                {
                    "mined" -> Stats.MINED as StatType<Any>
                    "crafted" -> Stats.CRAFTED as StatType<Any>
                    "used" -> Stats.USED as StatType<Any>
                    "broken" -> Stats.BROKEN as StatType<Any>
                    "picked_up" -> Stats.PICKED_UP as StatType<Any>
                    "dropped" -> Stats.DROPPED as StatType<Any>
                    "killed" -> Stats.KILLED as StatType<Any>
                    "killed_by" -> Stats.KILLED_BY as StatType<Any>
                    "custom" -> Stats.CUSTOM as StatType<Any>
                    else -> null
                }
            } catch (_: Exception)
            {
                return null
            }
        }
    }
}