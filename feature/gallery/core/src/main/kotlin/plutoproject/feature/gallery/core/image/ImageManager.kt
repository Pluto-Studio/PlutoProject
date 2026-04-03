package plutoproject.feature.gallery.core.image

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.time.delay
import plutoproject.feature.gallery.core.RESOURCE_CACHE_TTL_SECONDS
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.concurrent.Volatile
import kotlin.coroutines.CoroutineContext

class ImageRuntimeState(
    var image: Image? = null,
    var imageDataEntry: ImageDataEntry<*>? = null,
    var expiry: Instant? = null,
    var isPinned: Boolean = false,
)

private class LockHolder {
    val mutex = Mutex()
    var refCount = 0
}

class ImageManager(
    private val coroutineScope: CoroutineScope,
    private val coroutineContext: CoroutineContext,
    private val clock: Clock = Clock.systemUTC(),
    private val logger: Logger = Logger.getLogger(ImageManager::class.java.name),
    private val imageRepo: ImageRepository,
    private val imageDataRepo: ImageDataEntryRepository,
) {
    @Volatile
    private var isClosed = false
    private val lock = Any()
    private val runtimeStatesByImageId = mutableMapOf<UUID, ImageRuntimeState>()
    private val lockHoldersByImageId = mutableMapOf<UUID, LockHolder>()
    private var cleanerJob: Job? = null

    sealed interface CreateResult {
        data class Success(val image: Image) : CreateResult
        data object AlreadyExists : CreateResult
    }

    sealed interface DeleteResult {
        data class Success(val image: Image) : DeleteResult
        data object NotFound : DeleteResult
    }

    sealed interface CreateImageDataEntryResult<out T> {
        data class Success<T : Any>(val imageDataEntry: ImageDataEntry<T>) : CreateImageDataEntryResult<T>
        data object AlreadyExists : CreateImageDataEntryResult<Nothing>
    }

    sealed interface DeleteImageDataEntryResult {
        data object Success : DeleteImageDataEntryResult
        data object NotFound : DeleteImageDataEntryResult
    }

    private fun nextExpireTime(): Instant {
        return clock.instant().plusSeconds(RESOURCE_CACHE_TTL_SECONDS)
    }

    private fun ensureCleanerRunning() {
        if (cleanerJob?.isActive == true) {
            return
        }

        cleanerJob = coroutineScope.launch(coroutineContext) {
            runCleaner()
        }.also { job ->
            job.invokeOnCompletion { cause ->
                handleCleanerCompletion(cause)
            }
        }
    }

    private fun handleCleanerCompletion(cause: Throwable?) {
        if (cause == null || cause is InternalCleanerCancellation || !coroutineScope.isActive) {
            cleanerJob = null
            return
        }

        logger.log(
            Level.WARNING,
            "An internal error occurred while running cleaner job for ${this::class.simpleName}",
            cause
        )
        ensureCleanerRunning()
    }

    private suspend fun <T> withImageLock(id: UUID, block: suspend () -> T): T {
        val lockHolder = synchronized(lock) {
            lockHoldersByImageId.getOrPut(id) { LockHolder() }.also {
                it.refCount += 1
            }
        }
        return try {
            lockHolder.mutex.withLock { block() }
        } finally {
            cleanupOperationLockIfUnused(id, lockHolder)
        }
    }

    private suspend fun runCleaner() {
        while (true) {
            val nextCleanup = synchronized(lock) {
                runtimeStatesByImageId
                    .mapNotNull { (imageId, state) -> state.expiry?.let { imageId to it } }
                    .minByOrNull { (_, expiry) -> expiry }
            } ?: return

            val (imageId, expiry) = nextCleanup
            val now = clock.instant()
            if (now < expiry) {
                delay(Duration.between(now, expiry))
            }

            synchronized(lock) {
                if (isClosed || cleanerJob?.isActive != true) {
                    return
                }

                val state = runtimeStatesByImageId[imageId] ?: continue
                if (state.expiry != expiry) {
                    continue
                }
                if (state.isPinned) {
                    state.expiry = null
                    continue
                }

                runtimeStatesByImageId.remove(imageId)
            }
        }
    }

    suspend fun createImage(
        id: UUID,
        type: ImageType,
        owner: UUID,
        ownerName: String,
        name: String,
        widthBlocks: Int,
        heightBlocks: Int,
        tileMapIds: IntArray
    ): CreateResult {
        return withImageLock(id) {
            checkNotClosed()

            val existing = synchronized(lock) { runtimeStatesByImageId[id]?.image }
                ?: imageRepo.findById(id)
            if (existing != null) {
                return@withImageLock CreateResult.AlreadyExists
            }

            val image = Image(
                id = id,
                type = type,
                owner = owner,
                ownerName = ownerName,
                name = name,
                widthBlocks = widthBlocks,
                heightBlocks = heightBlocks,
                tileMapIds = tileMapIds,
            )
            imageRepo.save(image)
            cacheImage(image)
            CreateResult.Success(image)
        }
    }

    suspend fun getImage(id: UUID): Image? {
        return withImageLock(id) {
            synchronized(lock) {
                checkNotClosed()
                runtimeStatesByImageId[id]?.image?.let { return@withImageLock it }
            }

            val image = imageRepo.findById(id) ?: return@withImageLock null
            cacheImage(image)
            image
        }
    }

    suspend fun getImages(ids: Collection<UUID>): Map<UUID, Image> {
        if (ids.isEmpty()) {
            return emptyMap()
        }

        return ids.distinct().mapNotNull { imageId ->
            getImage(imageId)?.let { imageId to it }
        }.toMap()
    }

    suspend fun findImageByOwner(owner: UUID): Collection<Image> {
        checkNotClosed()
        val images = imageRepo.findByOwner(owner)
        images.forEach { image ->
            withImageLock(image.id) {
                val loaded = synchronized(lock) { runtimeStatesByImageId[image.id]?.image }
                    ?: imageRepo.findById(image.id)
                    ?: return@withImageLock
                cacheImage(loaded)
            }
        }
        return images
    }

    suspend fun deleteImage(id: UUID): DeleteResult {
        return withImageLock(id) {
            checkNotClosed()

            val image = synchronized(lock) { runtimeStatesByImageId[id]?.image }
                ?: imageRepo.findById(id)
                ?: return@withImageLock DeleteResult.NotFound

            imageRepo.deleteById(id)
            synchronized(lock) {
                checkNotClosed()
                val state = runtimeStatesByImageId[id] ?: return@synchronized
                state.image = null
                cleanupRuntimeStateIfEmpty(id, state)
            }
            DeleteResult.Success(image)
        }
    }

    suspend fun countImage(): Int {
        checkNotClosed()
        return imageRepo.count()
    }

    suspend fun <T : ImageData> createImageDataEntry(
        imageId: UUID,
        type: ImageType,
        data: T
    ): CreateImageDataEntryResult<T> {
        return withImageLock(imageId) {
            checkNotClosed()
            require(type == data.type) { "Image type $type does not match image data type ${data.type}" }

            val existing = synchronized(lock) { runtimeStatesByImageId[imageId]?.imageDataEntry }
                ?: imageDataRepo.findByImageId(imageId)
            if (existing != null) {
                return@withImageLock CreateImageDataEntryResult.AlreadyExists
            }

            val entry = createImageDataEntry(imageId, data)
            imageDataRepo.save(entry)
            cacheImageDataEntry(entry)
            CreateImageDataEntryResult.Success(entry)
        }
    }

    suspend fun getImageDataEntry(imageId: UUID): ImageDataEntry<*>? {
        return withImageLock(imageId) {
            synchronized(lock) {
                checkNotClosed()
                runtimeStatesByImageId[imageId]?.imageDataEntry?.let { return@withImageLock it }
            }

            val entry = imageDataRepo.findByImageId(imageId) ?: return@withImageLock null
            cacheImageDataEntry(entry)
            entry
        }
    }

    suspend fun getImageDataEntries(imageIds: Collection<UUID>): Map<UUID, ImageDataEntry<*>> {
        if (imageIds.isEmpty()) {
            return emptyMap()
        }

        return imageIds.distinct().mapNotNull { imageId ->
            getImageDataEntry(imageId)?.let { imageId to it }
        }.toMap()
    }

    suspend fun deleteImageDataEntry(imageId: UUID): DeleteImageDataEntryResult {
        return withImageLock(imageId) {
            checkNotClosed()

            val entry = synchronized(lock) { runtimeStatesByImageId[imageId]?.imageDataEntry }
                ?: imageDataRepo.findByImageId(imageId)
                ?: return@withImageLock DeleteImageDataEntryResult.NotFound

            imageDataRepo.deleteByImageId(imageId)
            synchronized(lock) {
                checkNotClosed()
                val state = runtimeStatesByImageId[imageId] ?: return@synchronized
                state.imageDataEntry = null
                cleanupRuntimeStateIfEmpty(imageId, state)
            }
            DeleteImageDataEntryResult.Success
        }
    }

    fun pin(imageId: UUID) {
        synchronized(lock) {
            checkNotClosed()
            val state = runtimeStatesByImageId[imageId] ?: return
            state.isPinned = true
            state.expiry = null
        }
    }

    fun unpin(imageId: UUID) {
        synchronized(lock) {
            checkNotClosed()
            val state = runtimeStatesByImageId[imageId] ?: return
            state.isPinned = false
            state.expiry = nextExpireTime()
            ensureCleanerRunning()
        }
    }

    fun close() {
        if (isClosed) {
            return
        }

        isClosed = true
        synchronized(lock) {
            runtimeStatesByImageId.clear()
            lockHoldersByImageId.clear()
        }
        cleanerJob?.cancel(InternalCleanerCancellation())
        cleanerJob = null
    }

    private fun cacheImage(image: Image): Image {
        synchronized(lock) {
            checkNotClosed()
            val state = runtimeStatesByImageId.getOrPut(image.id) { ImageRuntimeState() }
            state.image = image
            if (!state.isPinned) {
                state.expiry = state.expiry ?: nextExpireTime()
                ensureCleanerRunning()
            }
        }
        return image
    }

    private fun cacheImageDataEntry(entry: ImageDataEntry<*>): ImageDataEntry<*> {
        synchronized(lock) {
            checkNotClosed()
            val state = runtimeStatesByImageId.getOrPut(entry.imageId) { ImageRuntimeState() }
            state.imageDataEntry = entry
            if (!state.isPinned) {
                state.expiry = state.expiry ?: nextExpireTime()
                ensureCleanerRunning()
            }
        }
        return entry
    }

    private fun cleanupRuntimeStateIfEmpty(imageId: UUID, state: ImageRuntimeState) {
        if (state.image != null || state.imageDataEntry != null || state.isPinned) {
            return
        }
        runtimeStatesByImageId.remove(imageId)
    }

    private fun cleanupOperationLockIfUnused(imageId: UUID, lockHolder: LockHolder) {
        synchronized(lock) {
            lockHolder.refCount -= 1
            if (lockHolder.refCount > 0 || runtimeStatesByImageId.containsKey(imageId)) {
                return
            }
            lockHoldersByImageId.remove(imageId, lockHolder)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : ImageData> createImageDataEntry(imageId: UUID, data: T): ImageDataEntry<T> {
        return when (data) {
            is ImageData.Static -> ImageDataEntry.Static(imageId, data)
            is ImageData.Animated -> ImageDataEntry.Animated(imageId, data)
        } as ImageDataEntry<T>
    }

    private fun checkNotClosed() {
        check(!isClosed) { "Image manager already closed" }
    }

    private class InternalCleanerCancellation : CancellationException()
}
