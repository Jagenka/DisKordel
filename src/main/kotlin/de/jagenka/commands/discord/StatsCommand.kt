@file:Suppress("UNCHECKED_CAST")

package de.jagenka.commands.discord

import com.mojang.authlib.GameProfile
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.exceptions.CommandSyntaxException
import de.jagenka.DiscordHandler
import de.jagenka.PlayerStatManager
import de.jagenka.UserRegistry
import de.jagenka.commands.DiscordCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.argument
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal
import de.jagenka.commands.discord.MessageCommandSource.Companion.redirect
import net.minecraft.stat.Stat
import net.minecraft.stat.StatType
import net.minecraft.stat.Stats
import net.minecraft.util.Identifier

object StatsCommand : DiscordCommand
{
    fun getReplyForAll(statType: StatType<Any>, id: String): String
    {
        return getReplyForSome(UserRegistry.getMinecraftProfiles(), statType, id)
    }

    fun getReplyForSome(collection: Collection<GameProfile>, statType: StatType<Any>, id: String): String
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

            return collection
                .mapNotNull { it.name to (PlayerStatManager.getStatHandlerForPlayer(it.name)?.getStat(stat) ?: return@mapNotNull null) }
                .sortedByDescending { it.second }
                .filterNot { it.second == 0 }
                .joinToString(separator = "\n") { format(it.first, stat, it.second) }
                .replace("``````", "")
                .ifBlank { noNonZeroValuesFound }
        } catch (_: Exception)
        {
            return defaultResponse
        }
    }

    private fun format(playerName: String, stat: Stat<*>, value: Int) = "${"$playerName:".padEnd(17, ' ')} ${stat.format(value)}" // max length of player name is 16 character
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
                    .then(argument<String>("partOfPlayerName", StringArgumentType.word())
                        .executes {
                            val partOfPlayerName = it.getArgument("partOfPlayerName", String::class.java)
                            val statType = it.getArgument("statType", StatType::class.java) as StatType<Any>
                            val statIdentifier = it.getArgument("stat_identifier", String::class.java)
                            DiscordHandler.sendCodeBlock(
                                text = getReplyForSome(UserRegistry.findMinecraftProfiles(partOfPlayerName), statType, statIdentifier),
                                silent = true
                            )
                            0
                        }
                    )))
        )

        val alias = dispatcher.register(redirect("stats", commandNode))

        Registry.registerShortHelpText(shortHelpText, commandNode, alias)
        Registry.registerLongHelpText(longHelpText, commandNode, alias)
    }
}

class StatTypeArgument : ArgumentType<StatType<*>>
{
    override fun parse(reader: StringReader?): StatType<*>
    {
        if (reader == null) throw NullPointerException("StringReader should not be null (yei Java Inter-Op)")

        return when (reader.readUnquotedString())
        {
            "mined" -> Stats.MINED
            "crafted" -> Stats.CRAFTED
            "used" -> Stats.USED
            "broken" -> Stats.BROKEN
            "picked_up" -> Stats.PICKED_UP
            "dropped" -> Stats.DROPPED
            "killed" -> Stats.KILLED
            "killed_by" -> Stats.KILLED_BY
            "custom" -> Stats.CUSTOM
            else -> throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader)
        }
    }

    override fun getExamples(): MutableCollection<String> = mutableListOf("mined", "crafted", "used", "broken", "picked_up", "dropped", "killed", "killed_by", "custom")
}