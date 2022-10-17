package de.jagenka.commands

import com.mojang.brigadier.context.CommandContext
import de.jagenka.Users
import net.minecraft.server.command.ServerCommandSource


object WhoisCommand : StringInStringOutCommand
{
    override val literal: String
        get() = "whois"

    override fun execute(ctx: CommandContext<ServerCommandSource>, input: String): String
    {
        return Users.whoIsPrintable(input)
    }
}
