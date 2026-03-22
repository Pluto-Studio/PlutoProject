package plutoproject.feature.gallery.core.display

import plutoproject.feature.gallery.core.util.ChunkKey
import plutoproject.feature.gallery.core.util.ResourceEntry
import plutoproject.feature.gallery.core.util.ResourceHolder
import java.util.*

private data class DisplayInstanceIndex(
    val belongsTo: UUID,
    val chunkKey: ChunkKey
)

class DisplayInstanceResources : ResourceHolder<UUID, DisplayInstance> {
    private val lock = Any()
    private var isDisposed = false
    private val _entries = mutableMapOf<UUID, ResourceEntry<DisplayInstance>>()
    private val indexMetadataById = mutableMapOf<UUID, DisplayInstanceIndex>()
    private val idsByBelongsTo = mutableMapOf<UUID, MutableSet<UUID>>()
    private val idsByChunk = mutableMapOf<ChunkKey, MutableSet<UUID>>()

    override val entries: Map<UUID, ResourceEntry<DisplayInstance>>
        get() = synchronized(lock) { _entries.toMap() }

    override fun put(key: UUID, value: DisplayInstance) {
        check(key == value.id) { "Index and instance id mismatch" }
        synchronized(lock) {
            checkNotDisposed()
            if (_entries.containsKey(key)) {
                return@synchronized
            }
            _entries[key] = ResourceEntry(value)
            indexMetadataById[key] = DisplayInstanceIndex(
                belongsTo = value.belongsTo,
                chunkKey = value.chunkKey
            )
            loadIndexes(value)
        }
    }

    private val DisplayInstance.chunkKey: ChunkKey
        get() = ChunkKey(chunkX, chunkZ)

    private fun loadIndexes(instance: DisplayInstance) {
        idsByBelongsTo
            .getOrPut(instance.belongsTo) { mutableSetOf() }
            .add(instance.id)
        idsByChunk
            .getOrPut(instance.chunkKey) { mutableSetOf() }
            .add(instance.id)
    }

    override fun putAll(values: Map<UUID, DisplayInstance>) {
        if (values.isEmpty()) {
            return
        }
        synchronized(lock) {
            values.forEach(::put)
        }
    }

    override fun acquire(key: UUID): ResourceEntry.Handle<DisplayInstance>? {
        return synchronized(lock) {
            checkNotDisposed()
            _entries[key]?.acquire()
        }
    }

    override fun acquireBatch(keys: Collection<UUID>): Map<UUID, ResourceEntry.Handle<DisplayInstance>> {
        if (keys.isEmpty()) {
            return emptyMap()
        }
        return synchronized(lock) {
            checkNotDisposed()
            keys.mapNotNull { key ->
                _entries[key]?.acquire()?.let { key to it }
            }.toMap()
        }
    }

    override fun remove(key: UUID) {
        val entry = synchronized(lock) {
            checkNotDisposed()
            val removedEntry = _entries.remove(key) ?: return@synchronized null
            val indexMetadata = indexMetadataById.remove(key)
                ?: error("Unexpected: Index metadata not found for existing entry")
            unloadIndexes(key, indexMetadata)
            removedEntry
        }
        entry?.dispose()
    }

    private fun unloadIndexes(id: UUID, metadata: DisplayInstanceIndex) {
        idsByBelongsTo.computeIfPresent(metadata.belongsTo) { _, ids ->
            ids.remove(id)
            if (ids.isEmpty()) null else ids
        }
        idsByChunk.computeIfPresent(metadata.chunkKey) { _, ids ->
            ids.remove(id)
            if (ids.isEmpty()) null else ids
        }
    }

    override fun removeAll(keys: Collection<UUID>) {
        if (keys.isEmpty()) {
            return
        }
        val removedEntries = synchronized(lock) {
            checkNotDisposed()
            keys.mapNotNull { key ->
                val removedEntry = _entries.remove(key) ?: return@mapNotNull null
                val indexMetadata = indexMetadataById.remove(key)
                    ?: error("Unexpected: Index metadata not found for existing entry")
                unloadIndexes(key, indexMetadata)
                removedEntry
            }
        }
        removedEntries.forEach(ResourceEntry<DisplayInstance>::dispose)
    }

    override fun dispose() {
        val entriesToDispose = synchronized(lock) {
            if (isDisposed) {
                return@synchronized null
            }
            isDisposed = true
            clearIndexes()
            indexMetadataById.clear()
            _entries.values.toList().also { _entries.clear() }
        }
        entriesToDispose?.forEach(ResourceEntry<DisplayInstance>::dispose)
    }

    private fun clearIndexes() {
        idsByBelongsTo.clear()
        idsByChunk.clear()
    }

    private fun checkNotDisposed() {
        check(!isDisposed) { "Display instance resources already disposed" }
    }
}
