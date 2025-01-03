package plutoproject.framework.paper.util.world

import kotlinx.coroutines.future.await
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.level.chunk.status.ChunkStatus
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.craftbukkit.CraftChunk
import org.bukkit.craftbukkit.CraftWorld

suspend fun World.getChunkFromSource(x: Int, z: Int): Chunk? {
    val serverLevel = (this as CraftWorld).handle
    val chunkSource = serverLevel.chunkSource
    val result = chunkSource.getChunkFuture(x, z, ChunkStatus.FULL, true).await()
    val chunkAccess = result.orElse(null) ?: return null
    val levelChunk = chunkAccess as LevelChunk
    return CraftChunk(levelChunk)
}
