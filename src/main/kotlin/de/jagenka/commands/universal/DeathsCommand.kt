package de.jagenka.commands.universal

import de.jagenka.MinecraftHandler
import de.jagenka.UserRegistry

object DeathsCommand : StringInStringOutCommand // TODO: remove
{
    override val minecraftName: String
        get() = "deaths"
    override val ids: List<String>
        get() = listOf(minecraftName)
    override val helpText: String
        get() = "List how many deaths players have. No argument lists all players."
    override val variableName: String
        get() = "playerName"


    override fun process(input: String): String
    {
        return getDeathLeaderboardStrings(input.trim()).joinToString("\n").ifBlank { "No-one found!" }
    }

    /**
     * @return List of Pair of real playerName and deathCount
     */
    fun getDeathScores(input: String): List<Pair<String, Int>>
    {
        MinecraftHandler.minecraftServer?.let { server ->
            val result = mutableListOf<Pair<String, Int>>()

            val possiblePlayers = UserRegistry.find(input)

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