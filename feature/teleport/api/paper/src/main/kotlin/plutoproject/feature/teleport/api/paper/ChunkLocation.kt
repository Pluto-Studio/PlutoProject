package plutoproject.feature.teleport.api.paper

import org.bukkit.World
import org.bukkit.Chunk
import kotlinx.coroutines.future.await

@JvmInline
value class ChunkLocation(private val value: Long) {
    constructor(x: Int, z: Int) : this(x.toLong() shl 32 or z.toLong())

    val x: Int
        get() = (value ushr 32).toInt()
    val z: Int
        get() = value.toInt()

    fun isChunkLoaded(world: World): Boolean = world.isChunkLoaded(x, z)

    suspend fun coordinateChunkIn(world: World): Chunk = world.getChunkAtAsync(x, z).await()

    fun coordinateChunkInBlocking(world: World): Chunk = world.getChunkAtAsync(x, z).join()
}
