package plutoproject.framework.paper.util.world.location

import org.bukkit.Location

fun Location.toAngleErased(): Location {
    val rawLocation = clone().toBlockLocation()
    rawLocation.yaw = 0F
    rawLocation.pitch = 0F
    return rawLocation
}
