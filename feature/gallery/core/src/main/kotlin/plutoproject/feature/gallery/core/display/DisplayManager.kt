package plutoproject.feature.gallery.core.display

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import plutoproject.feature.gallery.core.display.job.DisplayJob
import plutoproject.feature.gallery.core.display.job.DisplayJobFactory
import plutoproject.feature.gallery.core.display.job.SendJob
import plutoproject.feature.gallery.core.display.job.SendJobFactory
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.util.ChunkKey
import plutoproject.feature.gallery.core.util.TtlCache
import java.time.Clock
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

private class LockHolder {
    val mutex = Mutex()
    var refCount = 0
}

class DisplayManager(
    coroutineScope: CoroutineScope,
    coroutineContext: CoroutineContext,
    private val clock: Clock,
    logger: Logger,
    private val instanceRepo: DisplayInstanceRepository,
    private val scheduler: DisplayScheduler,
    private val displayJobFactory: Lazy<DisplayJobFactory>,
    private val sendJobFactory: Lazy<SendJobFactory>,
) {
    private val lock = Any()
    private val instanceCache = DisplayInstanceCache(coroutineScope, coroutineContext, clock, logger)
    private val displayJobsByImageId = ConcurrentHashMap<UUID, DisplayJob>()
    private val sendJobsByPlayerId = ConcurrentHashMap<UUID, SendJob>()
    private val lockHoldersByInstanceId = mutableMapOf<UUID, LockHolder>()
    private var isClosed = false

    sealed interface CreateInstanceResult {
        data class Success(val instance: DisplayInstance) : CreateInstanceResult
        data class AlreadyExists(val instance: DisplayInstance) : CreateInstanceResult
    }

    sealed interface DeleteInstanceResult {
        data class Success(val instance: DisplayInstance) : DeleteInstanceResult
        data object NotFound : DeleteInstanceResult
    }

    sealed interface CreateDisplayJobResult {
        data class Success(val job: DisplayJob) : CreateDisplayJobResult
        data class AlreadyExists(val job: DisplayJob) : CreateDisplayJobResult
    }

    sealed interface StopDisplayJobResult {
        data class Success(val job: DisplayJob) : StopDisplayJobResult
        data object NotFound : StopDisplayJobResult
    }

    sealed interface StartSendJobResult {
        data class Success(val job: SendJob) : StartSendJobResult
        data class AlreadyStarted(val job: SendJob) : StartSendJobResult
    }

    sealed interface StopSendJobResult {
        data class Success(val job: SendJob) : StopSendJobResult
        data object NotStarted : StopSendJobResult
    }

    private suspend fun <T> withInstanceLock(id: UUID, block: suspend () -> T): T {
        val holder = synchronized(lock) {
            checkNotClosed()
            lockHoldersByInstanceId.getOrPut(id) { LockHolder() }.also {
                it.refCount += 1
            }
        }

        return try {
            holder.mutex.withLock { block() }
        } finally {
            cleanupOperationLockIfUnused(id, holder)
        }
    }

    suspend fun createInstance(instance: DisplayInstance): CreateInstanceResult {
        return withInstanceLock(instance.id) {
            checkNotClosed()

            val existing = instanceCache[instance.id] ?: instanceRepo.findById(instance.id)
            if (existing != null) {
                return@withInstanceLock CreateInstanceResult.AlreadyExists(existing)
            }

            instanceRepo.save(instance)
            instanceCache.put(instance)
            CreateInstanceResult.Success(instance)
        }
    }

    suspend fun getInstance(id: UUID): DisplayInstance? {
        return withInstanceLock(id) {
            checkNotClosed()

            instanceCache[id]?.let { return@withInstanceLock it }

            val instance = instanceRepo.findById(id) ?: return@withInstanceLock null
            instanceCache.put(instance)
            instance
        }
    }

    suspend fun findInstanceByImageId(imageId: UUID): List<DisplayInstance> {
        checkNotClosed()

        val loaded = instanceCache.findByImageId(imageId)
        if (loaded.isNotEmpty()) {
            return loaded
        }

        return instanceRepo.findByImageId(imageId).also(instanceCache::putAll)
    }

    suspend fun findInstanceByChunk(chunkX: Int, chunkZ: Int): List<DisplayInstance> {
        checkNotClosed()

        val loaded = instanceCache.findByChunk(chunkX, chunkZ)
        if (loaded.isNotEmpty()) {
            return loaded
        }

        return instanceRepo.findByChunk(chunkX, chunkZ).also(instanceCache::putAll)
    }

    suspend fun deleteInstance(id: UUID): DeleteInstanceResult {
        return withInstanceLock(id) {
            checkNotClosed()

            val instance = instanceCache[id] ?: instanceRepo.findById(id)
            ?: return@withInstanceLock DeleteInstanceResult.NotFound

            instanceRepo.deleteById(id)
            instanceCache.remove(id)
            DeleteInstanceResult.Success(instance)
        }
    }

    fun pinInstance(id: UUID) {
        checkNotClosed()
        instanceCache.pin(id)
    }

    fun unpinInstance(id: UUID) {
        checkNotClosed()
        instanceCache.unpin(id)
    }

    fun getDisplayJob(imageId: UUID): DisplayJob? {
        checkNotClosed()
        return displayJobsByImageId[imageId]
    }

    fun createDisplayJob(
        image: Image,
        imageDataEntry: ImageDataEntry<*>,
    ): CreateDisplayJobResult {
        checkNotClosed()
        validateJobInputs(image, imageDataEntry)

        val existing = displayJobsByImageId[image.id]
        if (existing != null) {
            return CreateDisplayJobResult.AlreadyExists(existing)
        }

        val job = displayJobFactory.value.create(image, imageDataEntry)
        displayJobsByImageId[image.id] = job
        return CreateDisplayJobResult.Success(job)
    }

    private fun validateJobInputs(
        image: Image,
        imageDataEntry: ImageDataEntry<*>,
    ) {
        require(image.id == imageDataEntry.imageId) {
            "ImageDataEntry imageId mismatch: image.id=${image.id}, imageId=${imageDataEntry.imageId}"
        }
        require(image.type == imageDataEntry.type) {
            "Image and ImageDataEntry type mismatch: image.type=${image.type}, entry.type=${imageDataEntry.type}"
        }
    }

    fun startDisplayJob(job: DisplayJob, awakeAt: Instant = clock.instant()) {
        checkNotClosed()
        require(displayJobsByImageId[job.imageId] === job) { "DisplayJob is not managed for imageId=${job.imageId}" }
        scheduler.scheduleAwakeAt(job, awakeAt)
    }

    fun stopDisplayJob(imageId: UUID): StopDisplayJobResult {
        checkNotClosed()
        val job = displayJobsByImageId.remove(imageId) ?: return StopDisplayJobResult.NotFound
        scheduler.unschedule(job)
        job.stop()
        return StopDisplayJobResult.Success(job)
    }

    fun getSendJob(playerId: UUID): SendJob? {
        checkNotClosed()
        return sendJobsByPlayerId[playerId]
    }

    fun startSendJob(playerId: UUID): StartSendJobResult {
        checkNotClosed()

        val existing = sendJobsByPlayerId[playerId]
        if (existing != null) {
            return StartSendJobResult.AlreadyStarted(existing)
        }

        val job = sendJobFactory.value.create(playerId)
        sendJobsByPlayerId[playerId] = job
        return StartSendJobResult.Success(job)
    }

    fun stopSendJob(playerId: UUID): StopSendJobResult {
        checkNotClosed()
        val job = sendJobsByPlayerId.remove(playerId) ?: return StopSendJobResult.NotStarted
        job.stop()
        return StopSendJobResult.Success(job)
    }

    fun close() {
        if (isClosed) {
            return
        }

        isClosed = true
        instanceCache.close()
        displayJobsByImageId.values.forEach(DisplayJob::stop)
        displayJobsByImageId.clear()
        sendJobsByPlayerId.values.forEach(SendJob::stop)
        sendJobsByPlayerId.clear()
        synchronized(lock) {
            lockHoldersByInstanceId.clear()
        }
    }

    private fun cleanupOperationLockIfUnused(id: UUID, holder: LockHolder) {
        synchronized(lock) {
            holder.refCount -= 1
            if (holder.refCount > 0 || instanceCache[id] != null) {
                return
            }
            lockHoldersByInstanceId.remove(id, holder)
        }
    }

    private fun checkNotClosed() {
        check(!isClosed) { "Display manager already closed" }
    }
}

private class DisplayInstanceCache(
    coroutineScope: CoroutineScope,
    coroutineContext: CoroutineContext,
    clock: Clock,
    logger: Logger,
) : TtlCache<UUID, DisplayInstance, DisplayInstanceCache.Indexes>(
    coroutineScope = coroutineScope,
    coroutineContext = coroutineContext,
    clock = clock,
    logger = logger,
) {
    private val instanceIdsByImageId = mutableMapOf<UUID, MutableSet<UUID>>()
    private val instanceIdsByChunk = mutableMapOf<ChunkKey, MutableSet<UUID>>()

    override fun keyOf(value: DisplayInstance): UUID = value.id

    override fun buildIndex(value: DisplayInstance): Indexes {
        return Indexes(
            imageId = value.imageId,
            chunk = ChunkKey(value.chunkX, value.chunkZ),
        )
    }

    override fun onEntryAdded(key: UUID, index: Indexes) {
        instanceIdsByImageId.getOrPut(index.imageId) { mutableSetOf() }.add(key)
        instanceIdsByChunk.getOrPut(index.chunk) { mutableSetOf() }.add(key)
    }

    override fun onEntryRemoved(key: UUID, index: Indexes) {
        instanceIdsByImageId.computeIfPresent(index.imageId) { _, ids ->
            ids.remove(key)
            ids.takeUnless(Set<UUID>::isEmpty)
        }
        instanceIdsByChunk.computeIfPresent(index.chunk) { _, ids ->
            ids.remove(key)
            ids.takeUnless(Set<UUID>::isEmpty)
        }
    }

    fun findByImageId(imageId: UUID): List<DisplayInstance> {
        return instanceIdsByImageId[imageId].orEmpty().mapNotNull(this::get)
    }

    fun findByChunk(chunkX: Int, chunkZ: Int): List<DisplayInstance> {
        return instanceIdsByChunk[ChunkKey(chunkX, chunkZ)].orEmpty().mapNotNull(this::get)
    }

    data class Indexes(
        val imageId: UUID,
        val chunk: ChunkKey,
    )
}
