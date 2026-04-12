package plutoproject.framework.paper.util.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.asCoroutineDispatcher
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.entity.Entity
import plutoproject.framework.paper.util.coroutine.dispatchers.ChunkDispatcher
import plutoproject.framework.paper.util.coroutine.dispatchers.EntityDispatcher
import plutoproject.framework.paper.util.coroutine.dispatchers.GlobalRegionDispatcher
import plutoproject.framework.paper.util.isFolia
import plutoproject.framework.paper.util.server
import plutoproject.framework.paper.util.toNms
import kotlin.coroutines.CoroutineContext

private object MainThreadCoroutineDispatcher : CoroutineDispatcher() {
    private val executorDispatcher = server.toNms().asCoroutineDispatcher()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (Bukkit.isPrimaryThread()) {
            block.run()
            return
        }
        executorDispatcher.dispatch(context, block)
    }
}

/**
 * 获取服务器的 [CoroutineContext]。
 *
 * 在 Paper 上为基于服务器 EventLoop 的 [CoroutineDispatcher]，在 Folia 上为 [GlobalRegionDispatcher]。
 */
val Server.coroutineContext: CoroutineContext
    get() = if (isFolia) GlobalRegionDispatcher else MainThreadCoroutineDispatcher

/**
 * 获取实体的 [CoroutineContext]。
 *
 * 在 Paper 上为基于服务器 EventLoop 的 [CoroutineDispatcher]，在 Folia 上为 [EntityDispatcher]。
 */
val Entity.coroutineContext: CoroutineContext
    get() = if (isFolia) EntityDispatcher(this) else server.coroutineContext

/**
 * 获取区块的 [CoroutineContext]。
 *
 * 在 Paper 上为基于服务器 EventLoop 的 [CoroutineDispatcher]，在 Folia 上为 [ChunkDispatcher]。
 */
val Chunk.coroutineContext: CoroutineContext
    get() = if (isFolia) ChunkDispatcher(this) else server.coroutineContext

/**
 * 获取该位置区块的 [CoroutineContext]。
 *
 * 在 Paper 上为基于服务器 EventLoop 的 [CoroutineDispatcher]，在 Folia 上为 [ChunkDispatcher]。
 */
val Location.coroutineContext: CoroutineContext
    get() = chunk.coroutineContext
