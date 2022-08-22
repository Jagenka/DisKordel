package de.jagenka

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import java.util.*

object Util
{
    fun Double.trim(digits: Int): String
    {
        return "%.${digits}f".format(this)
    }

    fun <T> Optional<T>.unwrap(): T? = orElse(null)

    fun sendChatMessage(message: String)
    {
        sendChatMessage(Text.of(message))
    }

    fun sendChatMessage(text: Text)
    {
        if (!HackfleischDiskursMod.checkMinecraftServer()) return
        HackfleischDiskursMod.minecraftServer.playerManager.broadcast(text, false)
    }

    fun ServerPlayerEntity.sendPrivateMessage(text: String)
    {
        this.sendMessage(Text.of(text))
    }

}