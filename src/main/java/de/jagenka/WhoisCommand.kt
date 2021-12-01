package de.jagenka

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.network.MessageType
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Util


object WhoisCommand
{
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>)
    {
        dispatcher.register(
            CommandManager.literal("whois").then(CommandManager.argument("name", StringArgumentType.greedyString()).executes
            {
                handleWhoisCommand(it, StringArgumentType.getString(it, "name"))
                return@executes 0;
            })
        )
    }

    private fun handleWhoisCommand(context: CommandContext<ServerCommandSource>, name: String)
    {
        context.source.player.sendMessage(Text.of(DiscordBot.whoIsUser(name)), MessageType.CHAT, Util.NIL_UUID)
    }
}
