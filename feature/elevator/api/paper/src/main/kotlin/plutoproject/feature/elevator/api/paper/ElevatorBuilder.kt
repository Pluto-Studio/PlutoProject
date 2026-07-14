package plutoproject.feature.elevator.api.paper

import org.bukkit.Location
import org.bukkit.Material

interface ElevatorBuilder {
    val type: Material
    val permission: String?

    suspend fun findLocations(startPoint: Location): List<Location>

    suspend fun teleportLocations(startPoint: Location): List<Location>
}
