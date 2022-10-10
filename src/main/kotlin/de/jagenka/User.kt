package de.jagenka

data class User(val username: String, val displayName: String, val minecraftName: String)
{
    val prettyComboName: String
        get() = "${this.username} (${this.displayName}) aka ${this.minecraftName}"
}