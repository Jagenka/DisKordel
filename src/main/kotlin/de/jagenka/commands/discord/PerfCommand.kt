package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import de.jagenka.DiscordHandler
import de.jagenka.MinecraftHandler
import de.jagenka.Util.trimDecimals
import de.jagenka.commands.DiscordCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal
import de.jagenka.commands.discord.MessageCommandSource.Companion.redirect

object PerfCommand : DiscordCommand
{
    override val shortHelpText: String
        get() = "query server performance"
    override val longHelpText: String
        get() = "query the server's current ticks per second (TPS) and milliseconds per tick (MSPT)."

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        val commandNode =
            dispatcher.register(literal("perf")
                .executes {
                    val performanceMetrics = MinecraftHandler.getPerformanceMetrics()
                    DiscordHandler.sendMessage("TPS: ${performanceMetrics.tps.toDouble().trimDecimals(1)} MSPT: ${performanceMetrics.mspt.trimDecimals(1)}", silent = true)
                    0
                })

        val aliasCommandNode = dispatcher.register(redirect("performance", commandNode))

        Registry.registerShortHelpText(shortHelpText, commandNode, aliasCommandNode)
        Registry.registerLongHelpText(longHelpText, commandNode, aliasCommandNode)
    }
}