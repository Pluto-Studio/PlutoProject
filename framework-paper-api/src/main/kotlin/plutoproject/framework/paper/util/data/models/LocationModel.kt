package plutoproject.framework.paper.util.data.models

import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.Location

fun Location.toModel(): LocationModel = LocationModel(world.name, x, y, z, yaw, pitch)

@Serializable
data class LocationModel(
    val world: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
) {
    fun toLocation(): Location? {
        return Location(Bukkit.getWorld(world) ?: return null, x, y, z, yaw, pitch)
    }
}
