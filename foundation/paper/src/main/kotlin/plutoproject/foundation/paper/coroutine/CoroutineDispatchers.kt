package plutoproject.foundation.paper.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.asCoroutineDispatcher
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.entity.Entity
import plutoproject.foundation.paper.coroutine.dispatchers.ChunkDispatcher
import plutoproject.foundation.paper.coroutine.dispatchers.EntityDispatcher
import plutoproject.foundation.paper.coroutine.dispatchers.GlobalRegionDispatcher
import kotlin.coroutines.CoroutineContext

private val isFolia = runCatching {
    Class.forName(
        "io.papermc.paper.threadedregions.RegionizedServer",
        false,
        Bukkit.getServer().javaClass.classLoader,
    )
}.isSuccess

private object MainThreadCoroutineDispatcher : CoroutineDispatcher() {
    private val delegate = (Bukkit.getServer() as CraftServer).server.asCoroutineDispatcher()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (Bukkit.isPrimaryThread()) {
            block.run()
        } else {
            delegate.dispatch(context, block)
        }
    }
}

/**
 * 获取服务器的 [CoroutineDispatcher]。
 *
 * 在 Paper 上为基于服务器 EventLoop 的 [CoroutineDispatcher]，在 Folia 上为 [GlobalRegionDispatcher]。
 */
val Server.coroutineDispatcher: CoroutineDispatcher
    get() = if (isFolia) GlobalRegionDispatcher else MainThreadCoroutineDispatcher

/**
 * 获取实体的 [CoroutineDispatcher]。
 *
 * 在 Paper 上为基于服务器 EventLoop 的 [CoroutineDispatcher]，在 Folia 上为 [EntityDispatcher]。
 */
val Entity.coroutineDispatcher: CoroutineDispatcher
    get() = if (isFolia) EntityDispatcher(this) else Bukkit.getServer().coroutineDispatcher

/**
 * 获取区块的 [CoroutineDispatcher]。
 *
 * 在 Paper 上为基于服务器 EventLoop 的 [CoroutineDispatcher]，在 Folia 上为 [ChunkDispatcher]。
 */
val Chunk.coroutineDispatcher: CoroutineDispatcher
    get() = if (isFolia) ChunkDispatcher(this) else Bukkit.getServer().coroutineDispatcher

/**
 * 获取该位置区块的 [CoroutineDispatcher]。
 *
 * 在 Paper 上为基于服务器 EventLoop 的 [CoroutineDispatcher]，在 Folia 上为 [ChunkDispatcher]。
 */
val Location.coroutineDispatcher: CoroutineDispatcher
    get() = chunk.coroutineDispatcher
