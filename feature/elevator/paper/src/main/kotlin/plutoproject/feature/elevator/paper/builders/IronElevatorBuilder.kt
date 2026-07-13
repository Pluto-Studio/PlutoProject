package plutoproject.feature.elevator.paper.builders

import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import kotlinx.coroutines.launch
import org.bukkit.Location
import org.bukkit.Material
import plutoproject.feature.elevator.api.paper.ElevatorBuilder
import plutoproject.foundation.paper.world.viewAligned
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.paper.PaperModuleContext

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
        val context = currentModuleContext() as PaperModuleContext
        val up = context.coroutineScope.launch(context.plugin.minecraftDispatcher) {
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

        val down = context.coroutineScope.launch(context.plugin.minecraftDispatcher) {
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
