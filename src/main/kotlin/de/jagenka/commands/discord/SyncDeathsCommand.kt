package de.jagenka.commands.discord

import de.jagenka.DiscordHandler
import de.jagenka.MinecraftHandler
import de.jagenka.MinecraftHandler.logger
import de.jagenka.config.StatEntry
import de.jagenka.config.StatManager
import dev.kord.core.event.message.MessageCreateEvent

object SyncDeathsCommand : DiscordCommand
{
    override val discordName: String
        get() = "syncdeaths"
    override val helpText: String
        get() = "`${DiscordCommandRegistry.commandPrefix}${discordName} scoreboardName`: override death count with values from scoreboard `scoreboardName`."
    override val needsAdmin: Boolean
        get() = true

    override fun execute(event: MessageCreateEvent, args: String)
    {
        MinecraftHandler.minecraftServer?.let { server ->
            server.scoreboard.getAllPlayerScores(server.scoreboard.getObjective(args.split(" ").firstOrNull() ?: "deaths"))
                .forEach {
                    logger.info("${it.playerName} - ${it.score}")
                    StatManager.statEntries.getOrPut(it.playerName) { StatEntry() }.deaths = it.score
                }
        }

        StatManager.store()

        DiscordHandler.reactConfirmation(event.message)
    }
}