package plutoproject.feature.gallery.core.image

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.dummyUuid
import plutoproject.feature.gallery.core.sampleImage
import plutoproject.feature.gallery.core.sampleStaticImageDataEntry
import plutoproject.feature.gallery.core.render.tile.TilePool
import plutoproject.feature.gallery.core.render.tile.TilePoolSnapshot
import kotlin.time.Duration.Companion.milliseconds

class ImageManagerTest {
    @Test
    fun `should manage image lifecycle independently from image data entry`() = runTest {
        val manager = ImageManager()
        val image = sampleImage(id = dummyUuid(10))
        val entry = ImageDataEntry(
            belongsTo = image.id,
            type = ImageType.STATIC,
            data = StaticImageData(
                tilePool = TilePool.fromSnapshot(TilePoolSnapshot(offsets = intArrayOf(0, 0), blob = byteArrayOf())),
                tileIndexes = ushortArrayOf(0u),
            ),
        )

        manager.loadImage(image)
        assertNotNull(manager.getLoadedImage(image.id))
        assertNull(manager.getLoadedImageDataEntry(image.id))

        manager.loadImageDataEntry(entry)
        assertNotNull(manager.getLoadedImage(image.id))
        assertNotNull(manager.getLoadedImageDataEntry(image.id))

        manager.unloadImage(image.id)
        assertNull(manager.getLoadedImage(image.id))
        assertNotNull(manager.getLoadedImageDataEntry(image.id))
    }

    @Test
    fun `should load from io through lifecycle getters`() = runTest {
        val manager = ImageManager()
        val image = sampleImage(id = dummyUuid(11))

        val loadedImage = manager.getImage(image.id) { image }
        assertEquals(image.id, loadedImage?.id)
        assertEquals(image.id, manager.getLoadedImage(image.id)?.id)

        val entry = ImageDataEntry(
            belongsTo = image.id,
            type = ImageType.ANIMATED,
            data = AnimatedImageData(
                frameCount = 2,
                duration = 100.milliseconds,
                tilePool = TilePool.fromSnapshot(TilePoolSnapshot(offsets = intArrayOf(0, 1), blob = byteArrayOf(1))),
                tileIndexes = ushortArrayOf(0u, 0u),
            ),
        )

        val loadedEntry = manager.getImageDataEntry(image.id) { entry }
        assertEquals(image.id, loadedEntry?.belongsTo)
        assertEquals(image.id, manager.getLoadedImageDataEntry(image.id)?.belongsTo)
    }

    @Test
    fun `should support batch image and image data entry cache helpers`() {
        val manager = ImageManager()
        val firstImage = sampleImage(id = dummyUuid(12))
        val secondImage = sampleImage(id = dummyUuid(13))
        val firstEntry = sampleStaticImageDataEntry(firstImage.id)
        val secondEntry = sampleStaticImageDataEntry(secondImage.id)

        manager.loadImages(listOf(firstImage, secondImage))
        manager.loadImageDataEntries(listOf(firstEntry, secondEntry))

        assertEquals(setOf(firstImage.id, secondImage.id), manager.getLoadedImages(listOf(firstImage.id, secondImage.id, dummyUuid(14))).keys)
        assertEquals(setOf(firstImage.id, secondImage.id), manager.getLoadedImageDataEntries(listOf(firstImage.id, secondImage.id, dummyUuid(15))).keys)

        assertEquals(2, manager.unloadImages(listOf(firstImage.id, secondImage.id)).size)
        assertEquals(2, manager.unloadImageDataEntries(listOf(firstImage.id, secondImage.id)).size)
        assertTrue(manager.getLoadedImages(listOf(firstImage.id, secondImage.id)).isEmpty())
        assertTrue(manager.getLoadedImageDataEntries(listOf(firstImage.id, secondImage.id)).isEmpty())
    }
}
