package de.jagenka

import java.util.UUID

data class User(val username: String, val displayName: String, val minecraftName: String)
{
    val prettyComboName: String
        get() = "${this.username} (${this.displayName}) aka ${this.minecraftName}"
}

data class MinecraftUser(val name: String, val uuid: UUID)