package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.MinecraftHandler
import de.jagenka.Util.trimDecimals
import de.jagenka.commands.discord.structure.ArgumentCombination
import de.jagenka.commands.discord.structure.ArgumentCombination.Companion.empty
import de.jagenka.commands.discord.structure.MessageCommand

object PerfCommand : MessageCommand
{
    override val ids: List<String>
        get() = listOf("perf")
    override val helpText: String
        get() = "Show current server performance metrics."
    override val allowedArgumentCombinations: List<ArgumentCombination>
        get() = listOf(empty(helpText) { event ->
            val performanceMetrics = MinecraftHandler.getPerformanceMetrics()
            DiscordHandler.sendMessage("TPS: ${performanceMetrics.tps.trimDecimals(1)} MSPT: ${performanceMetrics.mspt.trimDecimals(1)}")
            true
        })
}