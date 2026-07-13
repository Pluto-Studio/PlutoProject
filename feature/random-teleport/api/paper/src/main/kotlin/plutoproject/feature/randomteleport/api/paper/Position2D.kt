package plutoproject.feature.randomteleport.api.paper

import org.bukkit.Location

data class Position2D(val x: Double, val z: Double) {
    constructor(location: Location) : this(location.x, location.z)

    fun add(x: Double, z: Double): Position2D = copy(x = this.x + x, z = this.z + z)

    fun subtract(x: Double, z: Double): Position2D = copy(x = this.x - x, z = this.z - z)
}
