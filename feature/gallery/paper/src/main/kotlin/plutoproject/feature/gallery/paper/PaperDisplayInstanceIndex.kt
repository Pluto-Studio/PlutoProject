package plutoproject.feature.gallery.paper

import kotlinx.coroutines.withContext
import org.bukkit.Chunk
import org.bukkit.NamespacedKey
import org.bukkit.Server
import org.bukkit.persistence.PersistentDataType
import plutoproject.feature.gallery.common.DisplayInstanceIndex
import plutoproject.feature.gallery.core.util.ChunkKey
import java.nio.ByteBuffer
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

private val DISPLAY_INSTANCE_INDEX_KEY = NamespacedKey("plutoproject_gallery", "display_instance_index")

class PaperDisplayInstanceIndex(
    private val server: Server,
    private val logger: Logger,
    private val serverContext: CoroutineContext,
) : DisplayInstanceIndex {

    override suspend fun get(world: String, chunkKey: ChunkKey): Set<UUID> = withContext(serverContext) {
        val chunk = getLoadedChunk(world, chunkKey) ?: return@withContext emptySet()
        currentSet(chunk)
    }

    override suspend fun add(world: String, chunkKey: ChunkKey, id: UUID) {
        withContext(serverContext) {
            mutate(world, chunkKey) { add(id) }
        }
    }

    override suspend fun remove(world: String, chunkKey: ChunkKey, id: UUID) {
        withContext(serverContext) {
            mutate(world, chunkKey) { remove(id) }
        }
    }

    private fun mutate(world: String, chunkKey: ChunkKey, block: MutableSet<UUID>.() -> Unit) {
        val chunk = getLoadedChunk(world, chunkKey) ?: return
        val set = currentSet(chunk).toMutableSet()
        set.block()
        writeSet(chunk, set)
    }

    private fun currentSet(chunk: Chunk): Set<UUID> {
        val bytes = chunk.persistentDataContainer.get(
            DISPLAY_INSTANCE_INDEX_KEY,
            PersistentDataType.BYTE_ARRAY
        ) ?: return emptySet()

        return runCatching { decodeUuidSet(bytes) }
            .onFailure {
                logger.log(
                    Level.WARNING,
                    "Failed to decode display instance index for chunk: ${chunk.world.name} (${chunk.x}, ${chunk.z})",
                    it
                )
            }.getOrDefault(emptySet())
    }

    private fun writeSet(chunk: Chunk, set: Set<UUID>) {
        chunk.persistentDataContainer.set(
            DISPLAY_INSTANCE_INDEX_KEY,
            PersistentDataType.BYTE_ARRAY,
            encodeUuidSet(set)
        )
    }

    private fun getLoadedChunk(worldName: String, chunkKey: ChunkKey): Chunk? {
        val world = server.getWorld(worldName) ?: return null
        if (!world.isChunkLoaded(chunkKey.x, chunkKey.z)) return null
        return world.getChunkAt(chunkKey.x, chunkKey.z)
    }
}

private fun encodeUuidSet(uuids: Set<UUID>): ByteArray {
    val buffer = ByteBuffer.allocate(uuids.size * 16)
    uuids.forEach {
        buffer.putLong(it.mostSignificantBits)
        buffer.putLong(it.leastSignificantBits)
    }
    return buffer.array()
}

private fun decodeUuidSet(bytes: ByteArray): Set<UUID> {
    require(bytes.size % 16 == 0) {
        "Corrupted UUID set data: byte array length ${bytes.size} is not divisible by 16"
    }

    val buffer = ByteBuffer.wrap(bytes)
    return buildSet(bytes.size / 16) {
        while (buffer.remaining() >= 16) {
            add(UUID(buffer.long, buffer.long))
        }
    }
}
