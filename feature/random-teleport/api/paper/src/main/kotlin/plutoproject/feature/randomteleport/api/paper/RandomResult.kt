package plutoproject.feature.randomteleport.api.paper

import org.bukkit.Location

data class RandomResult(
    val attempts: Int,
    val location: Location?
)
