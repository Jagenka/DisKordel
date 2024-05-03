@file:Suppress("UNCHECKED_CAST")

package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.DiscordHandler
import de.jagenka.UserRegistry
import de.jagenka.commands.DiskordelSlashCommand
import de.jagenka.commands.DiskordelTextCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.argument
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal
import de.jagenka.commands.discord.MessageCommandSource.Companion.redirect
import de.jagenka.stats.StatTypeArgument
import de.jagenka.stats.StatUtil
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.*
import net.minecraft.stat.StatType

object StatsCommand : DiskordelTextCommand, DiskordelSlashCommand
{
    override val name: String
        get() = "stat"
    override val description: String
        get() = "Query Minecraft stats."

    override suspend fun build(builder: RootInputChatBuilder)
    {
        with(builder)
        {
            subCommand("help", "Get help with the suite of stat commands.")
            subCommand("get", "List players and their stat.") {
                string("relation", "In what relation to play time.")
                {
                    required = true
                    choice("default (no relation)", "DEFAULT")
                    choice("stat per playtime", "STAT_PER_TIME")
                    choice("playtime per stat", "TIME_PER_STAT")
                }
                string("category", "Which category to query from.")
                {
                    required = true
                    choice("custom", "custom")
                    choice("mined", "mined")
                    choice("crafted", "crafted")
                    choice("used (right-clicked)", "used")
                    choice("broken (tools)", "broken")
                    choice("picked up", "picked_up")
                    choice("dropped", "dropped")
                    choice("killed", "killed")
                    choice("killed by", "killed_by")
                }
                string("stat", "Which specific stat to query, using Minecraft's internal ids.")
                { required = true }
                integer("top_n", "How many entries to display starting from 1st.")
                {
                    required = false
                    minValue = 1
                }
                string("part_of_name", "Part of a player's name.")
                { required = false }
                boolean("ascending", "If the result should be sorted ascending, rather than descending.")
                { required = false }
            }
            subCommand("compare", "Compare stats between yourself and another player, or two other players.") {
                string("relation", "In what relation to play time.")
                {
                    required = true
                    choice("default (no relation)", "DEFAULT")
                    choice("stat per playtime", "STAT_PER_TIME")
                    choice("playtime per stat", "TIME_PER_STAT")
                }
                string("category", "Which category to query from.")
                {
                    required = true
                    choice("custom", "custom")
                    choice("mined", "mined")
                    choice("crafted", "crafted")
                    choice("used (right-clicked)", "used")
                    choice("broken (tools)", "broken")
                    choice("picked up", "picked_up")
                    choice("dropped", "dropped")
                    choice("killed", "killed")
                    choice("killed by", "killed_by")
                }
                string("stat", "Which specific stat to query, using Minecraft's internal ids.")
                { required = true }
                string("player", "Part of a player's name.")
                { required = true }
                string("player2", "Part of a player's name.")
                { required = false }
                boolean("ascending", "If the result should be sorted ascending, rather than descending.")
                { required = false }
            }
        }
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        with(event)
        {
            val subcommands = interaction.command.data.options.value?.map { it.name } ?: emptyList()
            when (subcommands.firstOrNull())
            {
                "help" ->
                {
                    interaction.respondEphemeral {
                        content = "See https://github.com/Jagenka/DisKordel/blob/master/manual/queryable_stats.md for help."
                    }
                }

                "get" ->
                {
                    val response = interaction.deferEphemeralResponse()
                    val relation = StatUtil.StatQueryType.valueOf(interaction.command.strings["relation"]!!)
                    val statType = StatTypeArgument.parse(interaction.command.strings["category"]!!) as? StatType<Any> ?: return // should never happen
                    val stat = interaction.command.strings["stat"]!!
                    val topN = interaction.command.integers["top_n"]
                    val partOfName = interaction.command.strings["part_of_name"]
                    val ascending = interaction.command.booleans["ascending"]
                    val nameFilter = if (partOfName != null) UserRegistry.findMinecraftProfiles(partOfName) else emptyList()
                    val reply = StatUtil.getStatReply(
                        statType = statType,
                        id = stat,
                        queryType = relation,
                        nameFilter = nameFilter,
                        topN = topN?.toInt(),
                        ascending = ascending,
                        invoker = UserRegistry.getGameProfile(UserRegistry.findUser(interaction.user.id)?.minecraft?.uuid),
                    )
                    response.respond { content = reply }
                }

                "compare" ->
                {
                    val response = interaction.deferEphemeralResponse()
                    val relation = StatUtil.StatQueryType.valueOf(interaction.command.strings["relation"]!!)
                    val statType = StatTypeArgument.parse(interaction.command.strings["category"]!!) as? StatType<Any> ?: return // should never happen
                    val stat = interaction.command.strings["stat"]!!
                    val ascending = interaction.command.booleans["ascending"]
                    val player1 = UserRegistry.findMostLikelyMinecraftName(interaction.command.strings["player"]!!)
                    val player2Input = interaction.command.strings["player2"]
                    val player2 =
                        if (player2Input != null) UserRegistry.findMostLikelyMinecraftName(player2Input)
                        else UserRegistry.findUser(interaction.user.id)?.minecraft?.name
                    if (player2 == null)
                    {
                        response.respond { content = "error finding player2" }
                        return
                    }
                    val players = UserRegistry.findMinecraftProfiles(player1).union(UserRegistry.findMinecraftProfiles(player2))
                    if (players.isEmpty())
                    {
                        response.respond { content = "no players found." }
                        return
                    }
                    val reply = StatUtil.getStatReply(
                        statType = statType,
                        id = stat,
                        queryType = relation,
                        nameFilter = players,
                        ascending = ascending,
                        topN = 2,
                        invoker = UserRegistry.getGameProfile(UserRegistry.findUser(interaction.user.id)?.minecraft?.uuid),
                    )
                    response.respond { content = reply }
                }

                else ->
                {
                    interaction.respondEphemeral { content = "error finding subcommand" }
                }
            }
            return
        }
    }
}
