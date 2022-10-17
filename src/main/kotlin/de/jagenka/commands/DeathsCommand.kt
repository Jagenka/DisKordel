package de.jagenka.commands

import com.mojang.brigadier.context.CommandContext
import de.jagenka.MinecraftHandler
import de.jagenka.Users
import net.minecraft.server.command.ServerCommandSource

object DeathsCommand : StringInStringOutCommand
{
    override val literal: String
        get() = "deaths"

    override fun execute(ctx: CommandContext<ServerCommandSource>): String
    {
        val nameToCount = getDeathScores(ctx.source.name).firstOrNull() ?: return "No-one found!"
        return "You have died ${nameToCount.second} time" + (if (nameToCount.second != 1) "s" else "") + "."
    }

    override fun execute(ctx: CommandContext<ServerCommandSource>, input: String): String
    {
        return getDeathLeaderboardStrings(input).joinToString("\n").ifBlank { "No-one found!" }
    }

    /**
     * @return List of Pair of real playerName and deathCount
     */
    fun getDeathScores(input: String): List<Pair<String, Int>> //TODO: get death count independent of scoreboard
    {
        MinecraftHandler.minecraftServer?.let { server ->
            val result = mutableListOf<Pair<String, Int>>()

            val possiblePlayers = Users.find(input)

            println(possiblePlayers)

            server.scoreboard.getAllPlayerScores(server.scoreboard.getObjective("deaths"))
                .forEach {
                    possiblePlayers.forEach { player ->
                        if (it.playerName.equals(player.minecraftName, ignoreCase = true))
                        {
                            result.add(it.playerName to it.score)
                        }
                    }
                    if (it.playerName.equals(input, ignoreCase = true)) result.add(it.playerName to it.score)
                }

            return result.toList().sortedByDescending { it.second }
        }

        return emptyList()
    }

    /**
     * @return List of human-readable "leaderboard" entries of how often they died
     */
    fun getDeathLeaderboardStrings(input: String): List<String>
    {
        val result = mutableListOf<String>()
        getDeathScores(input).forEach { (playerName, deaths) ->
            result.add("$playerName has died $deaths time" + (if (deaths != 1) "s" else "") + ".")
        }
        return result.toList()
    }
}