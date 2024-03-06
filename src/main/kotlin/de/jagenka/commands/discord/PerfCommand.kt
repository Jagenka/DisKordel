package de.jagenka.commands.discord

import com.mojang.brigadier.CommandDispatcher
import de.jagenka.DiscordHandler
import de.jagenka.MinecraftHandler
import de.jagenka.Util.trimDecimals
import de.jagenka.commands.DiskordelSlashCommand
import de.jagenka.commands.DiskordelTextCommand
import de.jagenka.commands.discord.MessageCommandSource.Companion.literal
import de.jagenka.commands.discord.MessageCommandSource.Companion.redirect
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.interaction.RootInputChatBuilder

object PerfCommand : DiskordelTextCommand, DiskordelSlashCommand
{
    override val name: String
        get() = "performance"
    override val description: String
        get() = "Query current server ticks per second (TPS) and milliseconds per tick (MSPT)."

    override suspend fun build(builder: RootInputChatBuilder)
    {
        // nothing needed
    }

    override suspend fun execute(event: ChatInputCommandInteractionCreateEvent)
    {
        event.interaction.respondEphemeral {
            content = getResponse()
        }
    }

    override val internalId: String
        get() = "perf"

    override val shortHelpText: String
        get() = "query server performance"
    override val longHelpText: String
        get() = "Query the server's current ticks per second (TPS) and milliseconds per tick (MSPT)."

    override fun registerWithDiscord(dispatcher: CommandDispatcher<MessageCommandSource>)
    {
        val commandNode =
            dispatcher.register(literal("perf")
                .executes {
                    DiscordHandler.sendMessage(getResponse(), silent = true)
                    0
                })

        val aliasCommandNode = dispatcher.register(redirect("performance", commandNode))

        Registry.registerShortHelpText(shortHelpText, commandNode, aliasCommandNode)
        Registry.registerLongHelpText(longHelpText, commandNode, aliasCommandNode)
    }

    private fun getResponse(): String
    {
        val performanceMetrics = MinecraftHandler.getPerformanceMetrics()
        return "TPS: ${performanceMetrics.tps.toDouble().trimDecimals(1)} MSPT: ${performanceMetrics.mspt.trimDecimals(1)}"
    }
}