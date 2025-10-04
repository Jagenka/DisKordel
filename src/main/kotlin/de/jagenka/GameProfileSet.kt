package de.jagenka

import com.mojang.authlib.GameProfile

/**
 * custom set of game profiles that only check for name.
 * (idk if checking uuid should be done, but one uuid and name should be dependent on each other)
 * when adding, if a profile with the same name exists, it gets replaced
 */
class GameProfileSet() : HashSet<GameProfile>()
{
    override fun add(element: GameProfile): Boolean
    {
        val profile = this.find { it.name == element.name }

        if (profile != null)
        {
            super.remove(profile)
        }

        super.add(element)
        return true
    }

    override fun addAll(elements: Collection<GameProfile>): Boolean
    {
        elements.forEach { this.add(it) }
        return true
    }
}