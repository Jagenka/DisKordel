package de.jagenka.stats

import net.minecraft.stat.Stat
import net.minecraft.util.Identifier

data class StatData(val identifier: Identifier, val stat: Stat<*>, val playerName: String, val value: Int)
