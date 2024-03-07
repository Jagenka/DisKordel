@file:Suppress("UNCHECKED_CAST")

package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import de.jagenka.DiscordHandler
import de.jagenka.DiscordHandler.asCodeBlock
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
import dev.kord.rest.builder.interaction.RootInputChatBuilder
import dev.kord.rest.builder.interaction.integer
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.interaction.subCommand
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
                integer("limit", "How many entries to display.")
                { required = false }
                string("part_of_name", "Part of a player's name.")
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
                    val limit = interaction.command.integers["limit"]
                    val partOfName = interaction.command.strings["part_of_name"]
                    val nameFilter = if (partOfName != null) UserRegistry.findMinecraftProfiles(partOfName).map { it.name } else emptyList()
                    val reply = StatUtil.getStatReply(
                        statType = statType,
                        id = stat,
                        queryType = relation,
                        nameFilter = nameFilter,
                        limit = limit?.toInt()
                    )
                    response.respond { content = reply.asCodeBlock() }
                }

                "compare" ->
                {
                    val response = interaction.deferEphemeralResponse()
                    val relation = StatUtil.StatQueryType.valueOf(interaction.command.strings["relation"]!!)
                    val statType = StatTypeArgument.parse(interaction.command.strings["category"]!!) as? StatType<Any> ?: return // should never happen
                    val stat = interaction.command.strings["stat"]!!
                    val player1 = UserRegistry.findMostLikelyMinecraftName(interaction.command.strings["player"]!!)
                    val player2Input = interaction.command.strings["player2"]
                    val player2 =
                        if (player2Input != null) UserRegistry.findMostLikelyMinecraftName(player2Input)
                        else UserRegistry.findUser(interaction.user.id)?.minecraft?.name
                    val players = listOfNotNull(player1, player2)
                    if (players.isEmpty())
                    {
                        response.respond {
                            content = "no players found."
                        }
                        return
                    }
                    val reply = StatUtil.getStatReply(
                        statType = statType,
                        id = stat,
                        queryType = relation,
                        nameFilter = players
                    )
                    response.respond { content = reply.asCodeBlock() }
                }

                else ->
                {
                    interaction.respondEphemeral { content = "error finding subcommand" }
                }
            }
            return
        }
    }

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
