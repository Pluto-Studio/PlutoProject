package plutoproject.foundation.paper.world

import org.bukkit.Location

fun Location.viewAligned(): Location = clone().toBlockLocation().apply {
    yaw = 0F
    pitch = 0F
}
