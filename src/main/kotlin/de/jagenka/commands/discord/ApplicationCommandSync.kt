package de.jagenka.commands.discord

import de.jagenka.MinecraftHandler
import de.jagenka.commands.DiskordelSlashCommand
import dev.kord.common.entity.DiscordApplicationCommand
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.rest.builder.interaction.GuildMultiApplicationCommandBuilder
import dev.kord.rest.json.request.ApplicationCommandCreateRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Keeps this guild's application commands in sync with the commands defined in this mod.
 *
 * Instead of tracking a manually maintained version number, the commands we want are compared to the commands Discord
 * actually has. Only if they really differ, all of them are overwritten in a single request. This means at most one GET
 * and one PUT per startup, no matter how many commands there are, and no way for the two sides to silently drift apart.
 */
object ApplicationCommandSync
{
    /**
     * fields Discord adds to its own representation of a command, which we never send ourselves.
     *
     * `default_permission` is deprecated and always echoed back by Discord, so it cannot be compared either.
     */
    private val serverSideOnlyFields = setOf("id", "application_id", "guild_id", "version", "default_permission")

    /**
     * values that mean the same as the field not being there at all
     */
    private val insignificantValues = setOf<JsonElement>(JsonNull, JsonPrimitive(false), JsonArray(emptyList()), JsonObject(emptyMap()))

    private val json = Json { encodeDefaults = false }

    /**
     * registers [commands] as guild application commands, but only if they are not already registered exactly like this
     */
    suspend fun syncGuildCommands(kord: Kord, guildId: Snowflake, commands: List<DiskordelSlashCommand>)
    {
        val wanted = GuildMultiApplicationCommandBuilder().apply {
            commands.forEach { cmd ->
                input(cmd.name, cmd.description) { cmd.build(this) }
            }
        }.build()

        // localizations are requested, so that they are part of the comparison, should we ever use them
        val registered = kord.rest.interaction.getGuildApplicationCommands(kord.resources.applicationId, guildId, withLocalizations = true)

        val changes = changesBetween(wanted, registered)

        if (changes.isEmpty())
        {
            MinecraftHandler.logger.info("${wanted.size} Discord Application Commands are up to date.")
            return
        }

        MinecraftHandler.logger.info("Updating Discord Application Commands (${changes.joinToString()})...")
        changes.forEach { MinecraftHandler.logger.debug("{}: wanted {}, registered {}", it.name, it.wanted, it.registered) }

        kord.rest.interaction.createGuildApplicationCommands(kord.resources.applicationId, guildId, wanted)

        MinecraftHandler.logger.info("${wanted.size} Discord Application Commands registered!")
    }

    /**
     * removes any global application commands this bot has registered.
     *
     * DisKordel only ever registers guild commands, so a global command showing up here is always a leftover
     * (e.g. from manual testing against the wrong scope, or an older version of the mod that still used global
     * commands) rather than something we want. Left alone, it keeps showing up in command autocomplete in every
     * guild the bot is in and errors out when used, since nothing handles it. This is checked on every startup,
     * but does nothing (no request at all) once it's clean, so a correctly set up bot never pays for it.
     */
    suspend fun purgeStaleGlobalCommands(kord: Kord)
    {
        val staleGlobalCommands = kord.rest.interaction.getGlobalApplicationCommands(kord.resources.applicationId)

        if (staleGlobalCommands.isEmpty()) return

        MinecraftHandler.logger.warn(
            "Found ${staleGlobalCommands.size} stale global Discord Application Command(s) " +
                    "(${staleGlobalCommands.joinToString { it.name }}) that DisKordel never registers as such. Removing them..."
        )

        kord.rest.interaction.createGlobalApplicationCommands(kord.resources.applicationId, emptyList())

        MinecraftHandler.logger.info("Stale global Discord Application Commands removed.")
    }

    /**
     * a command that is new (`+`), no longer wanted (`-`) or has changed (`~`)
     */
    private data class CommandChange(private val symbol: Char, val name: String, val wanted: JsonElement?, val registered: JsonElement?)
    {
        override fun toString() = "$symbol$name"
    }

    /**
     * @return every command that has to be added, removed or changed, empty if Discord already has exactly the commands we want
     */
    private fun changesBetween(wanted: List<ApplicationCommandCreateRequest>, registered: List<DiscordApplicationCommand>): List<CommandChange>
    {
        val wantedByName = wanted.associate { it.name to it.comparableForm() }
        val registeredByName = registered.associate { it.name to it.comparableForm() }

        return (wantedByName.keys + registeredByName.keys).sorted().mapNotNull { name ->
            val wantedForm = wantedByName[name]
            val registeredForm = registeredByName[name]

            when
            {
                registeredForm == null -> CommandChange('+', name, wantedForm, null)
                wantedForm == null -> CommandChange('-', name, null, registeredForm)
                wantedForm != registeredForm -> CommandChange('~', name, wantedForm, registeredForm)
                else -> null
            }
        }
    }

    private fun ApplicationCommandCreateRequest.comparableForm() = json.encodeToJsonElement(this).comparableForm()

    private fun DiscordApplicationCommand.comparableForm() = json.encodeToJsonElement(this).comparableForm()

    /**
     * reduces a serialized command to the parts that actually define it, so that the two differently shaped
     * representations (what we send vs. what Discord returns) become comparable:
     * - fields only Discord knows about are dropped
     * - a field left at its default (`null`, `false`, empty) is the same as it not being there
     * - numbers are compared by value, not by how they are written (`1` vs. `1.0`)
     *
     * field order is irrelevant, but the order of options and choices is not, as it is what Discord displays.
     */
    private fun JsonElement.comparableForm(): JsonElement = when (this)
    {
        is JsonObject -> JsonObject(
            entries
                .filterNot { (name, _) -> name in serverSideOnlyFields }
                .associate { (name, value) -> name to value.comparableForm() }
                .filterValues { it !in insignificantValues }
                .toSortedMap()
        )

        is JsonArray -> JsonArray(map { it.comparableForm() })

        JsonNull -> this

        is JsonPrimitive -> if (isString || booleanOrNull != null) this else JsonPrimitive(content.removeSuffix(".0"))
    }
}
