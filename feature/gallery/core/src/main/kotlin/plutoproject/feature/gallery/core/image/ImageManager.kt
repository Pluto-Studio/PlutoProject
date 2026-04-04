package plutoproject.feature.gallery.core.image

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import plutoproject.feature.gallery.core.util.TtlCache
import java.time.Clock
import java.util.UUID
import java.util.logging.Logger
import kotlin.concurrent.Volatile
import kotlin.coroutines.CoroutineContext

class ImageRuntimeState(
    var image: Image? = null,
    var imageDataEntry: ImageDataEntry<*>? = null,
)

private class LockHolder {
    val mutex = Mutex()
    var refCount = 0
}

class ImageManager(
    private val coroutineScope: CoroutineScope,
    private val coroutineContext: CoroutineContext,
    private val clock: Clock,
    private val logger: Logger,
    private val imageRepo: ImageRepository,
    private val imageDataRepo: ImageDataEntryRepository,
) {
    @Volatile
    private var isClosed = false
    private val lock = Any()
    private val runtimeStatesByImageId = ImageRuntimeStateCache(coroutineScope, coroutineContext, clock, logger)
    private val lockHoldersByImageId = mutableMapOf<UUID, LockHolder>()

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
        data class Success(val imageDataEntry: ImageDataEntry<*>) : DeleteImageDataEntryResult
        data object NotFound : DeleteImageDataEntryResult
    }

    suspend fun createImage(
        id: UUID,
        type: ImageType,
        owner: UUID,
        ownerName: String,
        name: String,
        widthBlocks: Int,
        heightBlocks: Int,
        tileMapIds: IntArray,
    ): CreateResult {
        return withImageLock(id) {
            checkNotClosed()

            val existing = runtimeStatesByImageId[id]?.image ?: imageRepo.findById(id)
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
            checkNotClosed()

            runtimeStatesByImageId[id]?.image?.let { return@withImageLock it }

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

        return imageRepo.findByOwner(owner).onEach { image ->
            withImageLock(image.id) {
                val loaded = runtimeStatesByImageId[image.id]?.image ?: imageRepo.findById(image.id) ?: return@withImageLock
                cacheImage(loaded)
            }
        }
    }

    suspend fun deleteImage(id: UUID): DeleteResult {
        return withImageLock(id) {
            checkNotClosed()

            val image = runtimeStatesByImageId[id]?.image ?: imageRepo.findById(id)
                ?: return@withImageLock DeleteResult.NotFound

            imageRepo.deleteById(id)
            runtimeStatesByImageId[id]?.let { state ->
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
        data: T,
    ): CreateImageDataEntryResult<T> {
        return withImageLock(imageId) {
            checkNotClosed()
            require(type == data.type) { "Image type $type does not match image data type ${data.type}" }

            val existing = runtimeStatesByImageId[imageId]?.imageDataEntry ?: imageDataRepo.findByImageId(imageId)
            if (existing != null) {
                return@withImageLock CreateImageDataEntryResult.AlreadyExists
            }

            val entry = createImageDataEntryInternal(imageId, data)
            imageDataRepo.save(entry)
            cacheImageDataEntry(entry)
            CreateImageDataEntryResult.Success(entry)
        }
    }

    suspend fun getImageDataEntry(imageId: UUID): ImageDataEntry<*>? {
        return withImageLock(imageId) {
            checkNotClosed()

            runtimeStatesByImageId[imageId]?.imageDataEntry?.let { return@withImageLock it }

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

            val entry = runtimeStatesByImageId[imageId]?.imageDataEntry ?: imageDataRepo.findByImageId(imageId)
                ?: return@withImageLock DeleteImageDataEntryResult.NotFound

            imageDataRepo.deleteByImageId(imageId)
            runtimeStatesByImageId[imageId]?.let { state ->
                state.imageDataEntry = null
                cleanupRuntimeStateIfEmpty(imageId, state)
            }
            DeleteImageDataEntryResult.Success(entry)
        }
    }

    fun pin(imageId: UUID) {
        checkNotClosed()
        runtimeStatesByImageId.pin(imageId)
    }

    fun unpin(imageId: UUID) {
        checkNotClosed()
        runtimeStatesByImageId.unpin(imageId)
    }

    fun close() {
        if (isClosed) {
            return
        }

        isClosed = true
        runtimeStatesByImageId.close()
        synchronized(lock) {
            lockHoldersByImageId.clear()
        }
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

    private fun cacheImage(image: Image): Image {
        val state = runtimeStatesByImageId[image.id] ?: ImageRuntimeState(image = image).also(runtimeStatesByImageId::put)
        state.image = image
        return image
    }

    private fun cacheImageDataEntry(entry: ImageDataEntry<*>): ImageDataEntry<*> {
        val state = runtimeStatesByImageId[entry.imageId]
            ?: ImageRuntimeState(imageDataEntry = entry).also(runtimeStatesByImageId::put)
        state.imageDataEntry = entry
        return entry
    }

    private fun cleanupRuntimeStateIfEmpty(imageId: UUID, state: ImageRuntimeState) {
        if (state.image != null || state.imageDataEntry != null) {
            return
        }
        runtimeStatesByImageId.remove(imageId)
    }

    private fun cleanupOperationLockIfUnused(imageId: UUID, lockHolder: LockHolder) {
        synchronized(lock) {
            lockHolder.refCount -= 1
            if (lockHolder.refCount > 0 || runtimeStatesByImageId[imageId] != null) {
                return
            }
            lockHoldersByImageId.remove(imageId, lockHolder)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : ImageData> createImageDataEntryInternal(imageId: UUID, data: T): ImageDataEntry<T> {
        return when (data) {
            is ImageData.Static -> ImageDataEntry.Static(imageId, data)
            is ImageData.Animated -> ImageDataEntry.Animated(imageId, data)
        } as ImageDataEntry<T>
    }

    private fun checkNotClosed() {
        check(!isClosed) { "Image manager already closed" }
    }

    private class ImageRuntimeStateCache(
        coroutineScope: CoroutineScope,
        coroutineContext: CoroutineContext,
        clock: Clock,
        logger: Logger,
    ) : TtlCache<UUID, ImageRuntimeState, Unit>(
        coroutineScope = coroutineScope,
        coroutineContext = coroutineContext,
        clock = clock,
        logger = logger,
    ) {
        override fun keyOf(value: ImageRuntimeState): UUID {
            return value.image?.id ?: value.imageDataEntry?.imageId
            ?: error("ImageRuntimeState requires image or imageDataEntry to determine key")
        }

        override fun buildIndex(value: ImageRuntimeState) = Unit
    }
}
