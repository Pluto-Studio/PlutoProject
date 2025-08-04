package plutoproject.framework.paper.util.world.chunk

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import kotlinx.coroutines.future.await
import org.bukkit.Chunk
import org.bukkit.World
import plutoproject.framework.common.util.coroutine.PluginScope

@JvmInline
value class ChunkLocation(private val value: Long) {
    constructor(x: Int, z: Int) : this(x.toLong() shl 32 or z.toLong())

    val x: Int
        get() = (value ushr 32).toInt()
    val z: Int
        get() = value.toInt()

    fun isChunkLoaded(world: World): Boolean {
        return world.isChunkLoaded(x, z)
    }

    suspend fun coordinateChunkIn(world: World): Chunk {
        return world.getChunkAtAsync(x, z).await()
    }

    fun coordinateChunkInAsync(world: World): Deferred<Chunk> = PluginScope.async {
        coordinateChunkIn(world)
    }

    fun coordinateChunkInBlocking(world: World): Chunk {
        return coordinateChunkInAsync(world).asCompletableFuture().join()
    }
}
