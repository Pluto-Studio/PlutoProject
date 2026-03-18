package plutoproject.feature.gallery.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DisplayManager {
    private val loadedDisplayInstances = ConcurrentHashMap<UUID, DisplayInstance>()
    private val loadedDisplayIdsByBelongsTo = ConcurrentHashMap<UUID, MutableSet<UUID>>()
    private val loadedDisplayIdsByChunk = ConcurrentHashMap<ChunkKey, MutableSet<UUID>>()
    private val operationLocks = ConcurrentHashMap<UUID, Mutex>()

    suspend fun <T> withDisplayInstanceOperationLock(id: UUID, action: suspend () -> T): T {
        val lock = operationLocks.computeIfAbsent(id) { Mutex() }
        return try {
            lock.withLock { action() }
        } finally {
            cleanupOperationLockIfUnused(id, lock)
        }
    }

    fun getLoadedDisplayInstance(id: UUID): DisplayInstance? {
        return loadedDisplayInstances[id]
    }

    fun getLoadedDisplayInstances(ids: Collection<UUID>): Map<UUID, DisplayInstance> {
        if (ids.isEmpty()) {
            return emptyMap()
        }

        return ids.mapNotNull { id ->
            loadedDisplayInstances[id]?.let { id to it }
        }.toMap()
    }

    fun getLoadedDisplayInstancesByBelongsTo(belongsTo: UUID): List<DisplayInstance> {
        return loadedDisplayIdsByBelongsTo[belongsTo]
            .orEmpty()
            .mapNotNull { loadedDisplayInstances[it] }
    }

    fun getLoadedDisplayInstancesByChunk(chunkX: Int, chunkZ: Int): List<DisplayInstance> {
        return loadedDisplayIdsByChunk[ChunkKey(chunkX, chunkZ)]
            .orEmpty()
            .mapNotNull { loadedDisplayInstances[it] }
    }

    fun loadDisplayInstance(displayInstance: DisplayInstance): DisplayInstance {
        val previous = loadedDisplayInstances.put(displayInstance.id, displayInstance)
        if (previous != null) {
            unloadIndexes(previous)
        }
        loadIndexes(displayInstance)
        return displayInstance
    }

    fun loadDisplayInstances(displayInstances: Collection<DisplayInstance>) {
        displayInstances.forEach(::loadDisplayInstance)
    }

    fun unloadDisplayInstance(id: UUID): DisplayInstance? {
        val removed = loadedDisplayInstances.remove(id) ?: return null
        unloadIndexes(removed)
        cleanupOperationLockIfUnused(id, operationLocks[id])
        return removed
    }

    fun unloadDisplayInstances(ids: Collection<UUID>): List<DisplayInstance> {
        return ids.mapNotNull(::unloadDisplayInstance)
    }

    suspend fun getDisplayInstance(
        id: UUID,
        loader: suspend (UUID) -> DisplayInstance?,
    ): DisplayInstance? {
        val loaded = loadedDisplayInstances[id]
        if (loaded != null) {
            return loaded
        }

        val displayInstance = loader(id) ?: return null
        return loadDisplayInstance(displayInstance)
    }

    suspend fun lookupDisplayInstancesByBelongsTo(
        belongsTo: UUID,
        loader: suspend (UUID) -> List<DisplayInstance>,
    ): List<DisplayInstance> {
        val loaded = getLoadedDisplayInstancesByBelongsTo(belongsTo)
        if (loaded.isNotEmpty()) {
            return loaded
        }

        val loadedFromStorage = loader(belongsTo)
        loadedFromStorage.forEach(::loadDisplayInstance)
        return loadedFromStorage
    }

    suspend fun lookupDisplayInstancesByChunk(
        chunkX: Int,
        chunkZ: Int,
        loader: suspend (Int, Int) -> List<DisplayInstance>,
    ): List<DisplayInstance> {
        val loaded = getLoadedDisplayInstancesByChunk(chunkX, chunkZ)
        if (loaded.isNotEmpty()) {
            return loaded
        }

        val loadedFromStorage = loader(chunkX, chunkZ)
        loadedFromStorage.forEach(::loadDisplayInstance)
        return loadedFromStorage
    }

    private fun loadIndexes(displayInstance: DisplayInstance) {
        loadedDisplayIdsByBelongsTo.computeIfAbsent(displayInstance.belongsTo) {
            ConcurrentHashMap.newKeySet()
        }.add(displayInstance.id)
        loadedDisplayIdsByChunk.computeIfAbsent(ChunkKey(displayInstance.chunkX, displayInstance.chunkZ)) {
            ConcurrentHashMap.newKeySet()
        }.add(displayInstance.id)
    }

    private fun unloadIndexes(displayInstance: DisplayInstance) {
        loadedDisplayIdsByBelongsTo.computeIfPresent(displayInstance.belongsTo) { _, ids ->
            ids.remove(displayInstance.id)
            if (ids.isEmpty()) {
                null
            } else {
                ids
            }
        }
        loadedDisplayIdsByChunk.computeIfPresent(ChunkKey(displayInstance.chunkX, displayInstance.chunkZ)) { _, ids ->
            ids.remove(displayInstance.id)
            if (ids.isEmpty()) {
                null
            } else {
                ids
            }
        }
    }

    private fun cleanupOperationLockIfUnused(id: UUID, lock: Mutex?) {
        if (lock == null || lock.isLocked || loadedDisplayInstances.containsKey(id)) {
            return
        }
        operationLocks.remove(id, lock)
    }

    private data class ChunkKey(val x: Int, val z: Int)
}
