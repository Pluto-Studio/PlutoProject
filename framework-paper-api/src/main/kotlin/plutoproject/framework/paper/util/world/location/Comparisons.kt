package plutoproject.framework.paper.util.world.location

import org.bukkit.Location

fun minLocationOf(a: Location, b: Location): Location =
    Location(
        a.world,
        minOf(a.x, b.x),
        minOf(a.y, b.y),
        minOf(a.z, b.z)
    )

fun maxLocationOf(a: Location, b: Location): Location =
    Location(
        a.world,
        maxOf(a.x, b.x),
        maxOf(a.y, b.y),
        maxOf(a.z, b.z)
    )

fun Location.blockEquals(other: Location): Boolean =
    blockX == other.blockX && blockY == other.blockY && blockZ == other.blockZ
