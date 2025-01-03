package plutoproject.framework.paper.util.world.chunk

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.await
import org.bukkit.Chunk
import org.bukkit.World
import plutoproject.framework.common.util.coroutine.runAsyncIO

@JvmInline
value class ChunkLocation(private val value: Long) {
    constructor(x: Int, y: Int) : this(x.toLong() shl 32 or y.toLong())

    val x: Int
        get() = (value ushr 32).toInt()
    val y: Int
        get() = value.toInt()

    fun isChunkLoaded(world: World): Boolean {
        return world.isChunkLoaded(x, y)
    }

    suspend fun coordinateChunkIn(world: World): Chunk {
        return world.getChunkAtAsync(x, y).await()
    }

    fun coordinateChunkInAsync(world: World): Deferred<Chunk> =
        runAsyncIO { coordinateChunkIn(world) }
}
