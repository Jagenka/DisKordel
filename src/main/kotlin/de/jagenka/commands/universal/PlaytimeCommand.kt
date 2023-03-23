package de.jagenka.commands.universal

import de.jagenka.UserRegistry
import de.jagenka.commands.discord.StatsCommand
import net.minecraft.stat.StatType
import net.minecraft.stat.Stats

object PlaytimeCommand : StringInStringOutCommand
{
    override val minecraftName: String
        get() = "playtime"
    override val ids: List<String>
        get() = listOf(minecraftName)
    override val helpText: String
        get() = "List how much players have played. No argument lists all players."
    override val variableName: String
        get() = "playerName"


    override fun process(input: String): String
    {
        return if (input.isBlank())
        {
            StatsCommand.getReplyForAll(Stats.CUSTOM as StatType<Any>, "play_time")
        } else
        {
            StatsCommand.getReplyForSome(UserRegistry.findMinecraftProfiles(input), Stats.CUSTOM as StatType<Any>, "play_time")
        }
    }
}