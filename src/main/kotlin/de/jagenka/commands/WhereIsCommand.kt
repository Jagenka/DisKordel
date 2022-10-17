package de.jagenka.commands

import com.mojang.brigadier.context.CommandContext
import de.jagenka.MinecraftHandler
import de.jagenka.Users
import net.minecraft.server.command.ServerCommandSource

object WhereIsCommand : StringInStringOutCommand
{
    override val literal: String
        get() = "whereis"

    override fun execute(ctx: CommandContext<ServerCommandSource>): String
    {
        return "Press F3, you dungus!"
    }

    override fun execute(ctx: CommandContext<ServerCommandSource>, input: String): String
    {
        val possibleUsers = Users.find(input)

        if (possibleUsers.isEmpty()) return "No-one found!"

        return possibleUsers.joinToString("\n") { user ->
            MinecraftHandler.getPlayerPosition(user.minecraftName)?.let {
                "${user.minecraftName} is at (${it.x.toInt()} ${it.y.toInt()} ${it.z.toInt()})."
            } ?: ""
        }
    }
}