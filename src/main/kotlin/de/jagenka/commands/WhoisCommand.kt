package de.jagenka.commands

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import de.jagenka.DiscordBot
import de.jagenka.HackfleischDiskursMod
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource


object WhoisCommand : Command
{
    override fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
    {
        dispatcher.register(
            CommandManager.literal("whois").then(CommandManager.argument("name", StringArgumentType.greedyString()).executes
            {
                handleWhoIsCommand(it, StringArgumentType.getString(it, "name"))
                return@executes 0
            })
        )
    }

    private fun handleWhoIsCommand(context: CommandContext<ServerCommandSource>, name: String)
    {
        val player = context.source.player ?: return
        HackfleischDiskursMod.sendMessageToPlayer(player, DiscordBot.whoIsUser(name))
    }
}
