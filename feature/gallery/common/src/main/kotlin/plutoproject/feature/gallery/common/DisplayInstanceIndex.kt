package plutoproject.feature.gallery.adapter.common

import plutoproject.feature.gallery.core.util.ChunkKey
import java.util.*

interface DisplayInstanceIndex {
    suspend fun get(world: String, chunkKey: ChunkKey): Set<UUID>

    suspend fun add(world: String, chunkKey: ChunkKey, id: UUID)

    suspend fun remove(world: String, chunkKey: ChunkKey, id: UUID)
}
