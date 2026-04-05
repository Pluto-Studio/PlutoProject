package plutoproject.feature.gallery.core.image

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import plutoproject.feature.gallery.core.util.TtlCache
import java.time.Clock
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

class ImageCombination(
    var image: Image? = null,
    var imageDataEntry: ImageDataEntry<*>? = null,
)

private class ImageCache(
    coroutineScope: CoroutineScope,
    coroutineContext: CoroutineContext,
    clock: Clock,
    logger: Logger,
) : TtlCache<UUID, ImageCombination, Unit>(
    coroutineScope = coroutineScope,
    coroutineContext = coroutineContext,
    clock = clock,
    logger = logger,
) {
    override fun keyOf(value: ImageCombination): UUID {
        return value.image?.id ?: value.imageDataEntry?.imageId
        ?: error("ImageRuntimeState requires image or imageDataEntry to determine key")
    }

    override fun buildIndex(value: ImageCombination) = Unit
}

private class LockHolder {
    val mutex = Mutex()
    var refCount = AtomicInteger(0)
}

class ImageManager(
    coroutineScope: CoroutineScope,
    coroutineContext: CoroutineContext,
    clock: Clock,
    logger: Logger,
    private val imageRepo: ImageRepository,
    private val imageDataRepo: ImageDataEntryRepository,
) {
    private val stateLock = Any()

    private var activeOps: Int = 0
    private var isClosed = false
    private var closeWaiter: CompletableFuture<Unit>? = null

    private val imageCache = ImageCache(coroutineScope, coroutineContext, clock, logger)
    private val lockHoldersByImageId = ConcurrentHashMap<UUID, LockHolder>()

    sealed class CreateResult {
        data class Success(val image: Image, val imageDataEntry: ImageDataEntry<*>) : CreateResult()
        data object AlreadyExists : CreateResult()
    }

    sealed class DeleteResult {
        data class Success(val image: Image, val imageDataEntry: ImageDataEntry<*>) : DeleteResult()
        data object NotFound : DeleteResult()
    }

    sealed interface SaveResult {
        data object Success : SaveResult
        data object NotFound : SaveResult
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
        data: ImageData,
    ): CreateResult = operate {
        return withImageLock(id) {
            require(type == data.type) { "Image type $type does not match image data type ${data.type}" }

            val existingImage = imageCache[id]?.image ?: imageRepo.findById(id)
            if (existingImage != null) {
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
            @Suppress("UNCHECKED_CAST")
            val entry = when (data) {
                is ImageData.Static -> ImageDataEntry.Static(id, data)
                is ImageData.Animated -> ImageDataEntry.Animated(id, data)
            } as ImageDataEntry<*>

            imageDataRepo.save(entry)
            imageRepo.save(image)
            cacheImage(image)
            cacheImageDataEntry(entry)
            CreateResult.Success(image, entry)
        }
    }

    suspend fun getImage(id: UUID): Image? = operate {
        return withImageLock(id) {
            imageCache[id]?.image?.let { return@withImageLock it }
            val image = imageRepo.findById(id) ?: return@withImageLock null
            cacheImage(image)
            image
        }
    }

    suspend fun getImages(ids: Collection<UUID>): Map<UUID, Image> = operate {
        if (ids.isEmpty()) return emptyMap()

        val uniqueIds = ids.distinct()
        val missingIds = mutableListOf<UUID>()

        uniqueIds.forEach { id ->
            val cached = imageCache[id]?.image
            if (cached == null) {
                missingIds.add(id)
            }
        }

        val loadedById = if (missingIds.isEmpty()) {
            emptyMap()
        } else {
            imageRepo.findByIds(missingIds)
        }

        val result = mutableMapOf<UUID, Image>()

        uniqueIds.forEach { id ->
            withImageLock(id) {
                val cached = imageCache[id]?.image
                if (cached != null) {
                    result[id] = cached
                    return@withImageLock
                }

                val loaded = loadedById[id] ?: return@withImageLock
                cacheImage(loaded)
                result[id] = loaded
            }
        }

        return result
    }

    suspend fun findImageByOwner(owner: UUID): Collection<Image> = operate {
        val result = mutableListOf<Image>()

        imageRepo.findByOwner(owner).forEach { image ->
            withImageLock(image.id) {
                val cached = imageCache[image.id]?.image
                if (cached != null) {
                    result.add(cached)
                    return@withImageLock
                }

                result.add(cacheImage(image))
            }
        }

        return result
    }

    suspend fun deleteImage(id: UUID): DeleteResult = operate {
        return withImageLock(id) {
            val image = imageCache[id]?.image ?: imageRepo.findById(id)
            ?: return@withImageLock DeleteResult.NotFound
            val entry = imageCache[id]?.imageDataEntry ?: imageDataRepo.findByImageId(id)
            ?: error("Image data entry for image $id is missing")

            imageRepo.deleteById(id)
            imageCache[id]?.let { state ->
                state.image = null
                cleanupImageCacheIfEmpty(id, state)
            }

            imageDataRepo.deleteByImageId(id)
            imageCache[id]?.let { state ->
                state.imageDataEntry = null
                cleanupImageCacheIfEmpty(id, state)
            }
            DeleteResult.Success(image, entry)
        }
    }

    suspend fun countImage(): Int = operate {
        return imageRepo.count()
    }

    suspend fun getImageDataEntry(imageId: UUID): ImageDataEntry<*>? = operate {
        return withImageLock(imageId) {
            imageCache[imageId]?.imageDataEntry?.let { return@withImageLock it }
            val entry = imageDataRepo.findByImageId(imageId) ?: return@withImageLock null
            cacheImageDataEntry(entry)
            entry
        }
    }

    suspend fun getImageDataEntries(imageIds: Collection<UUID>): Map<UUID, ImageDataEntry<*>> = operate {
        if (imageIds.isEmpty()) return emptyMap()

        val uniqueIds = imageIds.distinct()
        val missingIds = mutableListOf<UUID>()

        uniqueIds.forEach { id ->
            val cached = imageCache[id]?.imageDataEntry
            if (cached == null) {
                missingIds.add(id)
            }
        }

        val loadedById = if (missingIds.isEmpty()) {
            emptyMap()
        } else {
            imageDataRepo.findByImageIds(missingIds)
        }

        val result = mutableMapOf<UUID, ImageDataEntry<*>>()

        uniqueIds.forEach { id ->
            withImageLock(id) {
                val cached = imageCache[id]?.imageDataEntry
                if (cached != null) {
                    result[id] = cached
                    return@withImageLock
                }

                val loaded = loadedById[id] ?: return@withImageLock
                cacheImageDataEntry(loaded)
                result[id] = loaded
            }
        }

        return result
    }

    suspend fun saveImage(image: Image): SaveResult = operate {
        return withImageLock(image.id) {
            imageCache[image.id]?.image?.let { cached ->
                require(cached === image) { "Cached image with id ${image.id} is not the same object instance" }
            }

            if (!imageRepo.update(image)) {
                return@withImageLock SaveResult.NotFound
            }

            if (imageCache[image.id]?.image !== image) {
                cacheImage(image)
            }
            SaveResult.Success
        }
    }

    suspend fun saveImageDataEntry(imageDataEntry: ImageDataEntry<*>): SaveResult = operate {
        return withImageLock(imageDataEntry.imageId) {
            imageCache[imageDataEntry.imageId]?.imageDataEntry?.let { cached ->
                require(cached === imageDataEntry) {
                    "Cached image data entry with imageId ${imageDataEntry.imageId} is not the same object instance"
                }
            }

            if (!imageDataRepo.update(imageDataEntry)) {
                return@withImageLock SaveResult.NotFound
            }

            if (imageCache[imageDataEntry.imageId]?.imageDataEntry !== imageDataEntry) {
                cacheImageDataEntry(imageDataEntry)
            }
            SaveResult.Success
        }
    }

    suspend fun pin(imageId: UUID) = operate {
        withImageLock(imageId) {
            imageCache.pin(imageId)
        }
    }

    suspend fun unpin(imageId: UUID) = operate {
        withImageLock(imageId) {
            imageCache.unpin(imageId)
        }
    }

    fun close() {
        synchronized(stateLock) {
            if (isClosed) return
            isClosed = true

            if (activeOps == 0) {
                null
            } else {
                CompletableFuture<Unit>().also { closeWaiter = it }
            }
        }?.join()

        imageCache.close()
        lockHoldersByImageId.clear()
    }

    private fun cacheImage(image: Image): Image {
        val state = imageCache[image.id]
            ?: ImageCombination(image = image).also(imageCache::put)
        state.image = image
        return image
    }

    private fun cacheImageDataEntry(entry: ImageDataEntry<*>): ImageDataEntry<*> {
        val state = imageCache[entry.imageId]
            ?: ImageCombination(imageDataEntry = entry).also(imageCache::put)
        state.imageDataEntry = entry
        return entry
    }

    private fun cleanupImageCacheIfEmpty(imageId: UUID, state: ImageCombination) {
        if (state.image != null || state.imageDataEntry != null) {
            return
        }
        imageCache.remove(imageId)
    }

    private fun ensureActive() {
        check(!isClosed) { "Manager is closed" }
    }

    private inline fun <T> operate(block: () -> T): T {
        synchronized(stateLock) {
            ensureActive()
            activeOps++
        }

        try {
            return block()
        } finally {
            synchronized(stateLock) {
                activeOps--
                if (isClosed && activeOps == 0) {
                    closeWaiter?.also { closeWaiter = null }
                } else {
                    null
                }
            }?.complete(Unit)
        }
    }

    private suspend fun <T> withImageLock(id: UUID, block: suspend () -> T): T {
        val lockHolder = lockHoldersByImageId.computeIfAbsent(id) { LockHolder() }
        lockHolder.refCount.incrementAndGet()

        return try {
            lockHolder.mutex.withLock { block() }
        } finally {
            cleanupImageLockIfUnused(id, lockHolder)
        }
    }

    private fun cleanupImageLockIfUnused(imageId: UUID, lockHolder: LockHolder) {
        val remaining = lockHolder.refCount.decrementAndGet()
        if (remaining > 0 || imageCache[imageId] != null) {
            return
        }
        lockHoldersByImageId.remove(imageId, lockHolder)
    }

}
