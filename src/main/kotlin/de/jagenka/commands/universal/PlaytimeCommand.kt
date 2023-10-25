package de.jagenka.commands.universal

import de.jagenka.commands.StringInStringOutCommand

object PlaytimeCommand : StringInStringOutCommand("playtime")
{
    override fun process(input: String): String
    {
        return ""/*if (input.isBlank())
        {
            StatsCommand.getReplyForAll(Stats.CUSTOM as StatType<Any>, "play_time")
        } else
        {
            StatsCommand.getReplyForSome(UserRegistry.findMinecraftProfiles(input), Stats.CUSTOM as StatType<Any>, "play_time")
        }*/
    }
}