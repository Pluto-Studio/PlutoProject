package plutoproject.feature.gallery.core.image

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.dummyUuid
import plutoproject.feature.gallery.core.newImageManager
import plutoproject.feature.gallery.core.sampleImage
import plutoproject.feature.gallery.core.sampleStaticImageDataEntry
import plutoproject.feature.gallery.core.schedulerClock

class ImageManagerTest {
    @Test
    fun `should create get and delete image`() = runTest {
        val manager = newImageManager(clock = schedulerClock(this))
        val image = sampleImage(id = dummyUuid(10))

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
            )
            assertEquals(image.id, (result as ImageManager.CreateResult.Success).image.id)
            assertNotNull(manager.getImage(image.id))
            val deleted = manager.deleteImage(image.id)
            assertEquals(image.id, (deleted as ImageManager.DeleteResult.Success).image.id)
            assertNull(manager.getImage(image.id))
        } finally {
            manager.close()
        }
    }

    @Test
    fun `should manage image data entry lifecycle independently`() = runTest {
        val manager = newImageManager(clock = schedulerClock(this))
        val entry = sampleStaticImageDataEntry(dummyUuid(20))

        try {
            val created = manager.createImageDataEntry(entry.imageId, entry.type, entry.asStatic().data)
            assertEquals(entry.imageId, (created as ImageManager.CreateImageDataEntryResult.Success).imageDataEntry.imageId)
            assertNotNull(manager.getImageDataEntry(entry.imageId))
            val deleted = manager.deleteImageDataEntry(entry.imageId)
            assertEquals(entry.imageId, (deleted as ImageManager.DeleteImageDataEntryResult.Success).imageDataEntry.imageId)
            assertNull(manager.getImageDataEntry(entry.imageId))
        } finally {
            manager.close()
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
