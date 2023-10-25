package de.jagenka.commands.universal

import de.jagenka.commands.StringInStringOutCommand

object DeathsCommand : StringInStringOutCommand("deaths")
{
    override fun process(input: String): String
    {
        return ""/*if (input.isBlank())
        {
            StatsCommand.getReplyForAll(Stats.CUSTOM as StatType<Any>, "deaths")
        } else
        {
            StatsCommand.getReplyForSome(UserRegistry.findMinecraftProfiles(input), Stats.CUSTOM as StatType<Any>, "deaths")
        }*/
    }
}