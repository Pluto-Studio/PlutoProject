package plutoproject.foundation.paper.coroutine.dispatchers

import io.papermc.paper.threadedregions.scheduler.RegionScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import org.bukkit.Bukkit
import org.bukkit.Chunk
import kotlin.coroutines.CoroutineContext

/**
 * 基于 Folia [RegionScheduler] 的 [CoroutineDispatcher]。
 */
class ChunkDispatcher(private val chunk: Chunk) : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        Bukkit.getRegionScheduler().execute(schedulerPlugin, chunk.world, chunk.x, chunk.z, block)
    }
}
