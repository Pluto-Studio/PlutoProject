package plutoproject.feature.gallery.core.display

import kotlinx.coroutines.CoroutineScope
import plutoproject.feature.gallery.core.util.CacheEntry
import plutoproject.feature.gallery.core.util.ChunkKey
import plutoproject.feature.gallery.core.util.ResourceCache
import java.time.Clock
import java.util.*
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

data class DisplayInstanceIndex(
    val belongsTo: UUID,
    val chunkKey: ChunkKey
)

class DisplayInstanceCache(
    coroutineScope: CoroutineScope,
    coroutineContext: CoroutineContext,
    clock: Clock, logger: Logger
) : ResourceCache<UUID, DisplayInstance, DisplayInstanceIndex>(coroutineScope, coroutineContext, clock, logger) {
    private val idsByBelongsTo = mutableMapOf<UUID, MutableSet<UUID>>()
    private val idsByChunk = mutableMapOf<ChunkKey, MutableSet<UUID>>()

    override fun keyOf(value: DisplayInstance): UUID = value.id

    override fun buildIndex(value: DisplayInstance): DisplayInstanceIndex = DisplayInstanceIndex(
        belongsTo = value.belongsTo,
        chunkKey = value.chunkKey
    )

    fun acquireByBelongsTo(belongsTo: UUID): Collection<CacheEntry.Handle<DisplayInstance>> {
        return withLock {
            val ids = idsByBelongsTo[belongsTo] ?: return@withLock emptyList()
            acquireBatch(ids).values
        }
    }

    fun acquireByChunk(chunkKey: ChunkKey): Collection<CacheEntry.Handle<DisplayInstance>> {
        return withLock {
            val ids = idsByChunk[chunkKey] ?: return@withLock emptyList()
            acquireBatch(ids).values
        }
    }

    override fun onEntryAdded(key: UUID, index: DisplayInstanceIndex) {
        idsByBelongsTo
            .getOrPut(index.belongsTo) { mutableSetOf() }
            .add(key)
        idsByChunk
            .getOrPut(index.chunkKey) { mutableSetOf() }
            .add(key)
    }

    override fun onEntryRemoved(key: UUID, index: DisplayInstanceIndex) {
        idsByBelongsTo.computeIfPresent(index.belongsTo) { _, ids ->
            ids.remove(key)
            if (ids.isEmpty()) null else ids
        }
        idsByChunk.computeIfPresent(index.chunkKey) { _, ids ->
            ids.remove(key)
            if (ids.isEmpty()) null else ids
        }
    }

    override fun onCacheDisposed() {
        idsByBelongsTo.clear()
        idsByChunk.clear()
    }
}

private val DisplayInstance.chunkKey: ChunkKey
    get() = ChunkKey(chunkX, chunkZ)
