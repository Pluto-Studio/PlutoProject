package plutoproject.framework.paper.util.world

import org.bukkit.util.BoundingBox
import org.bukkit.util.VoxelShape

fun BoundingBox.subtract(other: BoundingBox): Collection<BoundingBox> {
    if (!overlaps(other)) {
        return listOf(this)
    }

    val intersection = clone().intersection(other)
    val remaining = mutableListOf<BoundingBox>()

    if (intersection.minX > minX) {
        remaining.add(BoundingBox(minX, minY, minZ, intersection.minX, maxY, maxZ))
    }
    if (intersection.minY > minY) {
        remaining.add(BoundingBox(minX, minY, minZ, maxX, intersection.minY, maxZ))
    }
    if (intersection.minZ > minZ) {
        remaining.add(BoundingBox(minX, minY, minZ, maxX, maxY, intersection.minZ))
    }
    if (intersection.maxX < maxX) {
        remaining.add(BoundingBox(intersection.maxX, minY, minZ, maxX, maxY, maxZ))
    }
    if (intersection.maxY < maxY) {
        remaining.add(BoundingBox(minX, intersection.maxY, minZ, maxX, maxY, maxZ))
    }
    if (intersection.maxZ < maxZ) {
        remaining.add(BoundingBox(minX, minY, intersection.maxZ, maxX, maxY, maxZ))
    }

    return remaining
}

fun BoundingBox.subtract(others: Iterable<BoundingBox>): Collection<BoundingBox> {
    var remaining = listOf(this)
    others.forEach { other ->
        val newRemaining = mutableListOf<BoundingBox>()
        remaining.forEach { aabb ->
            newRemaining.addAll(aabb.subtract(other))
        }
        remaining = newRemaining
    }
    return remaining
}

fun BoundingBox.subtract(other: VoxelShape): Collection<BoundingBox> {
    return subtract(other.boundingBoxes)
}
