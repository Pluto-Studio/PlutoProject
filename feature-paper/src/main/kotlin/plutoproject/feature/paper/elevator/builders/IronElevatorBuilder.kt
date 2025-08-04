package plutoproject.feature.paper.elevator.builders

import kotlinx.coroutines.launch
import org.bukkit.Location
import org.bukkit.Material
import plutoproject.feature.paper.api.elevator.ElevatorBuilder
import plutoproject.framework.common.util.coroutine.PluginScope
import plutoproject.framework.paper.util.coroutine.coroutineContext
import plutoproject.framework.paper.util.world.location.viewAligned

@Suppress("UNUSED")
object IronElevatorBuilder : ElevatorBuilder {
    override val type: Material = Material.IRON_BLOCK
    override val permission: String? = null

    override suspend fun findLocations(startPoint: Location): List<Location> {
        val loc = startPoint.viewAligned()
        val offsetUp = mutableListOf<Location>()
        val offsetDown = mutableListOf<Location>()
        val result = mutableListOf<Location>()

        // TODO: 重写
        // 我怎么看不懂我自己写的代码了...
        val up = PluginScope.launch(loc.coroutineContext) {
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

        val down = PluginScope.launch(loc.coroutineContext) {
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
