package ink.pmc.essentials.api.teleport.random

import org.bukkit.Location

data class RandomResult(
    val attempts: Int,
    val location: Location?
)