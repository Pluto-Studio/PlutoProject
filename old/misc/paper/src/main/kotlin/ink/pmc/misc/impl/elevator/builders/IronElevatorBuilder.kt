package ink.pmc.misc.impl.elevator.builders

import ink.pmc.misc.api.elevator.ElevatorBuilder
import ink.pmc.framework.concurrent.submitSync
import ink.pmc.framework.world.eraseAngle
import org.bukkit.Location
import org.bukkit.Material

@Suppress("UNUSED")
object IronElevatorBuilder : ElevatorBuilder {
    override val type: Material = Material.IRON_BLOCK
    override val permission: String? = null

    override suspend fun findLocations(startPoint: Location): List<Location> {
        val loc = startPoint.eraseAngle()
        val offsetUp = mutableListOf<Location>()
        val offsetDown = mutableListOf<Location>()
        val result = mutableListOf<Location>()

        val up = loc.submitSync {
            val top = loc.world.maxHeight
            val curr = loc.blockY
            val temp = mutableListOf<Location>()
            for (i in loc.blockY..top) {
                val offset = i - curr
                val block = loc.clone().add(0.0, offset.toDouble(), 0.0)
                if (block.block.type != type) continue
                temp.add(block)
            }
            offsetUp.addAll(filterSafe(temp))
        }

        val down = loc.submitSync {
            val bottom = loc.world.minHeight
            val curr = loc.blockY
            val temp = mutableListOf<Location>()
            for (i in bottom..loc.blockY) {
                val offset = curr - i
                val block = loc.clone().subtract(0.0, offset.toDouble(), 0.0)
                if (block.block.type != type) continue
                temp.add(block)
            }
            offsetDown.addAll(filterSafe(temp))
        }

        up.join()
        down.join()
        result.addAll(offsetDown)
        result.addAll(offsetUp)
        return result
    }

    override suspend fun teleportLocations(startPoint: Location): List<Location> {
        return findLocations(startPoint).map {
            it.clone().add(0.0, 1.0, 0.0)
        }
    }

    private fun filterSafe(list: List<Location>): List<Location> {
        val filtered = list.filter {
            val offset1 = it.clone().add(0.0, 1.0, 0.0)
            val offset2 = it.clone().add(0.0, 2.0, 0.0)
            offset1.block.type.isAir && offset2.block.type.isAir
        }
        return filtered
    }
}