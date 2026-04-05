package plutoproject.feature.gallery.core.image

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.InMemoryImageDataEntryRepository
import plutoproject.feature.gallery.core.InMemoryImageRepository
import plutoproject.feature.gallery.core.dummyUuid
import plutoproject.feature.gallery.core.newImageManager
import plutoproject.feature.gallery.core.sampleImage
import plutoproject.feature.gallery.core.sampleStaticImageDataEntry
import plutoproject.feature.gallery.core.schedulerClock

class ImageManagerTest {
    @Test
    fun `should create and delete image with image data entry together`() = runTest {
        val manager = newImageManager(clock = schedulerClock(this))
        val image = sampleImage(id = dummyUuid(10))
        val entry = sampleStaticImageDataEntry(image.id)

        try {
            val result = manager.createImage(
                id = image.id,
                type = image.type,
                owner = image.owner,
                ownerName = image.ownerName,
                name = image.name,
                widthBlocks = image.widthBlocks,
                heightBlocks = image.heightBlocks,
                tileMapIds = image.tileMapIds,
                data = entry.asStatic().data,
            )
            result as ImageManager.CreateResult.Success
            assertEquals(image.id, result.image.id)
            assertEquals(image.id, result.imageDataEntry.imageId)
            assertNotNull(manager.getImage(image.id))
            assertNotNull(manager.getImageDataEntry(image.id))

            val deleted = manager.deleteImage(image.id)
            deleted as ImageManager.DeleteResult.Success
            assertEquals(image.id, deleted.image.id)
            assertEquals(image.id, deleted.imageDataEntry.imageId)
            assertNull(manager.getImage(image.id))
            assertNull(manager.getImageDataEntry(image.id))
        } finally {
            manager.close()
        }
    }

    @Test
    fun `save functions should return not found when repository entry does not exist`() = runTest {
        val manager = newImageManager(clock = schedulerClock(this))
        val image = sampleImage(id = dummyUuid(20))
        val entry = sampleStaticImageDataEntry(image.id)

        try {
            assertEquals(ImageManager.SaveResult.NotFound, manager.saveImage(image))
            assertEquals(ImageManager.SaveResult.NotFound, manager.saveImageDataEntry(entry))
        } finally {
            manager.close()
        }
    }

    @Test
    fun `save functions should reject different object instances when cache already has one`() = runTest {
        val manager = newImageManager(clock = schedulerClock(this))
        val image = sampleImage(id = dummyUuid(21))
        val entry = sampleStaticImageDataEntry(image.id)

        try {
            manager.createImage(
                id = image.id,
                type = image.type,
                owner = image.owner,
                ownerName = image.ownerName,
                name = image.name,
                widthBlocks = image.widthBlocks,
                heightBlocks = image.heightBlocks,
                tileMapIds = image.tileMapIds,
                data = entry.asStatic().data,
            )

            val anotherImage = sampleImage(id = image.id, owner = image.owner, ownerName = image.ownerName, name = image.name)
            val anotherEntry = sampleStaticImageDataEntry(image.id)

            try {
                manager.saveImage(anotherImage)
                fail("Expected saveImage to reject different cached instance")
            } catch (_: IllegalArgumentException) {
            }

            try {
                manager.saveImageDataEntry(anotherEntry)
                fail("Expected saveImageDataEntry to reject different cached instance")
            } catch (_: IllegalArgumentException) {
            }
        } finally {
            manager.close()
        }
    }

    @Test
    fun `save functions should cache object when it was not cached`() = runTest {
        var imageFindHits = 0
        var imageDataFindHits = 0
        val imageRepo = object : InMemoryImageRepository() {
            override suspend fun findById(id: java.util.UUID): Image? {
                imageFindHits += 1
                return super.findById(id)
            }
        }
        val imageDataRepo = object : InMemoryImageDataEntryRepository() {
            override suspend fun findByImageId(imageId: java.util.UUID): ImageDataEntry<*>? {
                imageDataFindHits += 1
                return super.findByImageId(imageId)
            }
        }
        val manager = newImageManager(
            clock = schedulerClock(this),
            imageRepo = imageRepo,
            imageDataRepo = imageDataRepo,
        )
        val image = sampleImage(id = dummyUuid(22))
        val entry = sampleStaticImageDataEntry(image.id)

        try {
            imageRepo.save(image)
            imageDataRepo.save(entry)

            image.changeOwnerName("Owner_456")

            assertEquals(ImageManager.SaveResult.Success, manager.saveImage(image))
            assertEquals(ImageManager.SaveResult.Success, manager.saveImageDataEntry(entry))

            assertNotNull(manager.getImage(image.id))
            assertNotNull(manager.getImageDataEntry(image.id))
            assertEquals(0, imageFindHits)
            assertEquals(0, imageDataFindHits)
        } finally {
            manager.close()
        }
    }

    @Test
    fun `batch getters should reload entry when cached value expires before lock acquisition`() = runTest {
        var singleImageFindHits = 0
        val imageRepo = object : InMemoryImageRepository() {
            override suspend fun findByIds(ids: Collection<java.util.UUID>): Map<java.util.UUID, Image> {
                testScheduler.advanceTimeBy(31_000)
                advanceUntilIdle()
                return super.findByIds(ids)
            }

            override suspend fun findById(id: java.util.UUID): Image? {
                singleImageFindHits += 1
                return super.findById(id)
            }
        }
        val imageManager = newImageManager(
            clock = schedulerClock(this),
            imageRepo = imageRepo,
            imageDataRepo = InMemoryImageDataEntryRepository(),
        )
        val cachedImage = sampleImage(id = dummyUuid(23))
        val cachedEntry = sampleStaticImageDataEntry(cachedImage.id)
        val otherImage = sampleImage(id = dummyUuid(24))

        try {
            imageManager.createImage(
                id = cachedImage.id,
                type = cachedImage.type,
                owner = cachedImage.owner,
                ownerName = cachedImage.ownerName,
                name = cachedImage.name,
                widthBlocks = cachedImage.widthBlocks,
                heightBlocks = cachedImage.heightBlocks,
                tileMapIds = cachedImage.tileMapIds,
                data = cachedEntry.asStatic().data,
            )
            imageRepo.save(otherImage)
            singleImageFindHits = 0

            val images = imageManager.getImages(listOf(cachedImage.id, otherImage.id))

            assertEquals(setOf(cachedImage.id, otherImage.id), images.keys)
            assertEquals(1, singleImageFindHits)
        } finally {
            imageManager.close()
        }

        var singleImageDataFindHits = 0
        val imageDataRepo = object : InMemoryImageDataEntryRepository() {
            override suspend fun findByImageIds(imageIds: Collection<java.util.UUID>): Map<java.util.UUID, ImageDataEntry<*>> {
                testScheduler.advanceTimeBy(31_000)
                advanceUntilIdle()
                return super.findByImageIds(imageIds)
            }

            override suspend fun findByImageId(imageId: java.util.UUID): ImageDataEntry<*>? {
                singleImageDataFindHits += 1
                return super.findByImageId(imageId)
            }
        }
        val entryManager = newImageManager(
            clock = schedulerClock(this),
            imageRepo = InMemoryImageRepository(),
            imageDataRepo = imageDataRepo,
        )
        val cachedImageForEntry = sampleImage(id = dummyUuid(25))
        val cachedEntryForEntry = sampleStaticImageDataEntry(cachedImageForEntry.id)
        val otherImageForEntry = sampleImage(id = dummyUuid(26))
        val otherEntry = sampleStaticImageDataEntry(otherImageForEntry.id)

        try {
            entryManager.createImage(
                id = cachedImageForEntry.id,
                type = cachedImageForEntry.type,
                owner = cachedImageForEntry.owner,
                ownerName = cachedImageForEntry.ownerName,
                name = cachedImageForEntry.name,
                widthBlocks = cachedImageForEntry.widthBlocks,
                heightBlocks = cachedImageForEntry.heightBlocks,
                tileMapIds = cachedImageForEntry.tileMapIds,
                data = cachedEntryForEntry.asStatic().data,
            )
            imageDataRepo.save(otherEntry)
            singleImageDataFindHits = 0

            val entries = entryManager.getImageDataEntries(listOf(cachedImageForEntry.id, otherImageForEntry.id))

            assertEquals(setOf(cachedImageForEntry.id, otherImageForEntry.id), entries.keys)
            assertEquals(1, singleImageDataFindHits)
        } finally {
            entryManager.close()
        }
    }

    @Test
    fun `pin should keep cached image until unpin and ttl cleanup`() = runTest {
        var repoHits = 0
        val repo = object : plutoproject.feature.gallery.core.InMemoryImageRepository() {
            override suspend fun findById(id: java.util.UUID) = super.findById(id).also {
                repoHits += 1
            }
        }
        val manager = newImageManager(clock = schedulerClock(this), imageRepo = repo)
        val image = sampleImage(id = dummyUuid(30))

        try {
            manager.createImage(
                id = image.id,
                type = image.type,
                owner = image.owner,
                ownerName = image.ownerName,
                name = image.name,
                widthBlocks = image.widthBlocks,
                heightBlocks = image.heightBlocks,
                tileMapIds = image.tileMapIds,
                data = sampleStaticImageDataEntry(image.id).asStatic().data,
            )
            val repoHitsAfterCreate = repoHits
            manager.pin(image.id)

            testScheduler.advanceTimeBy(31_000)
            advanceUntilIdle()
            manager.getImage(image.id)
            assertEquals(repoHitsAfterCreate, repoHits)

            manager.unpin(image.id)
            testScheduler.advanceTimeBy(31_000)
            advanceUntilIdle()
            manager.getImage(image.id)
            assertEquals(repoHitsAfterCreate + 1, repoHits)
        } finally {
            manager.close()
        }
    }
}
