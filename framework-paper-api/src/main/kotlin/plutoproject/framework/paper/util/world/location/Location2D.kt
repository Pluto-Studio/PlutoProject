package plutoproject.framework.paper.util.world.location

import org.bukkit.Location
import org.bukkit.World

data class Location2D(val world: World, val x: Double, val z: Double) {
    constructor(location: Location) : this(location.world, location.x, location.z)

    fun add(x: Double, z: Double): Location2D = copy(
        x = this.x + x,
        z = this.z + z,
    )

    fun subtract(x: Double, z: Double): Location2D = copy(
        x = this.x - x,
        z = this.z - z,
    )
}
