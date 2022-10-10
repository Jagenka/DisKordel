package de.jagenka.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import de.jagenka.Main
import de.jagenka.Users
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text

object DeathsCommand : Command
{
    override fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
    {
        dispatcher.register(
            CommandManager.literal("deaths")
                .executes {
                    handle(it, "")
                    return@executes 0
                }
                .then(
                    CommandManager.argument("name", StringArgumentType.greedyString()).executes
                    {
                        handle(it, StringArgumentType.getString(it, "name"))
                        return@executes 0
                    })
        )
    }

    private fun handle(it: CommandContext<ServerCommandSource>, input: String)
    {
        getDeathLeaderboardStrings(input).forEach { line ->
            it.source.sendFeedback(Text.literal(line), false)
        }
    }

    /**
     * @return List of Pair of real playerName and deathCount
     */
    fun getDeathScores(input: String): List<Pair<String, Int>> //TODO: get death count independent of scoreboard
    {
        Main.minecraftServer?.let { server ->
            val result = mutableListOf<Pair<String, Int>>()

            val possiblePlayers = Users.find(input)

            server.scoreboard.getAllPlayerScores(server.scoreboard.getObjective("deaths"))
                .forEach {
                    possiblePlayers.forEach { player ->
                        if (it.playerName.equals(player.minecraftName, ignoreCase = true)) result.add(it.playerName to it.score)
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
            result.add("$playerName has died $deaths time" + if (deaths != 1) "s" else "")
        }
        return result.toList()
    }
}