package de.jagenka.commands.discord

import com.mojang.authlib.Agent
import com.mojang.authlib.GameProfile
import com.mojang.authlib.ProfileLookupCallback
import de.jagenka.MinecraftHandler.minecraftServer
import de.jagenka.commands.discord.structure.ArgumentCombination
import de.jagenka.commands.discord.structure.MessageCommand

object TestCommand : MessageCommand
{
    override val ids: List<String>
        get() = listOf("test")
    override val helpText: String
        get() = "for testing"
    override val allowedArgumentCombinations: List<ArgumentCombination>
        get() = listOf(ArgumentCombination.empty("") {
            run()
            true
        })

    private fun run()
    {
        minecraftServer?.let {
            it.gameProfileRepo.findProfilesByNames(arrayOf("hideoturismo"), Agent.MINECRAFT,
                object : ProfileLookupCallback
                {
                    override fun onProfileLookupSucceeded(profile: GameProfile?)
                    {
                        println(profile)
                    }

                    override fun onProfileLookupFailed(profile: GameProfile?, exception: Exception?)
                    {
                        println("not found!")
                    }
                }
            )
        }
    }
}