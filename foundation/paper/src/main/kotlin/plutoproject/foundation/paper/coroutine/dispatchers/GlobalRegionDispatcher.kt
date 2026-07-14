package plutoproject.foundation.paper.coroutine.dispatchers

import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import org.bukkit.Bukkit
import kotlin.coroutines.CoroutineContext

/**
 * 基于 Folia [GlobalRegionScheduler] 的 [CoroutineDispatcher]。
 */
object GlobalRegionDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        Bukkit.getGlobalRegionScheduler().execute(schedulerPlugin, block)
    }
}
