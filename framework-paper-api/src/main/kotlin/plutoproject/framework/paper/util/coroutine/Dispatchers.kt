package plutoproject.framework.paper.util.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.entity.Entity
import plutoproject.framework.paper.util.isFolia
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server
import plutoproject.framework.paper.util.toNms
import kotlin.coroutines.CoroutineContext

private val GLOBAL_REGION_DISPATCHER = object : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        server.globalRegionScheduler.execute(plugin, block)
    }
}

val Server.coroutineContext: CoroutineContext
    get() = if (isFolia) {
        GLOBAL_REGION_DISPATCHER
    } else {
        server.toNms().asCoroutineDispatcher()
    }

val Entity.coroutineContext: CoroutineContext
    get() = if (isFolia) {
        object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                this@coroutineContext.scheduler.execute(plugin, block, {}, 0L)
            }
        }
    } else {
        server.coroutineContext
    }

val Chunk.coroutineContext: CoroutineContext
    get() = if (isFolia) {
        object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                val chunk = this@coroutineContext
                server.regionScheduler.execute(plugin, chunk.world, chunk.x, chunk.z, block)
            }
        }
    } else {
        server.coroutineContext
    }

val Location.coroutineContext
    get() = chunk.coroutineContext
