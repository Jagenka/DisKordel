package de.jagenka.commands.universal

import de.jagenka.MinecraftHandler
import de.jagenka.Users
import de.jagenka.config.StatManager

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

            val possiblePlayers = Users.find(input)

            StatManager.statEntries
                .forEach {
                    possiblePlayers.forEach { player ->
                        if (it.key.equals(player.minecraftName, ignoreCase = true))
                        {
                            result.add(it.key to it.value.deaths)
                        }
                    }
                    if (it.key.equals(input, ignoreCase = true)) result.add(it.key to it.value.deaths)
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