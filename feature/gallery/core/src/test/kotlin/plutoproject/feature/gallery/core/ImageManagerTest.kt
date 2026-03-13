package plutoproject.feature.gallery.core

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ImageManagerTest {
    @Test
    fun `should manage image lifecycle independently from image data entry`() = runTest {
        val manager = ImageManager()
        val image = sampleImage(id = dummyUuid(10))
        val entry = ImageDataEntry(
            belongsTo = image.id,
            type = ImageType.STATIC,
            data = StaticImageData(
                tilePool = TilePool(offsets = intArrayOf(0, 0), blob = byteArrayOf()),
                tileIndexes = shortArrayOf(0),
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
                durationMillis = 100,
                tilePool = TilePool(offsets = intArrayOf(0, 1), blob = byteArrayOf(1)),
                tileIndexes = shortArrayOf(0, 0),
            ),
        )

        val loadedEntry = manager.getImageDataEntry(image.id) { entry }
        assertEquals(image.id, loadedEntry?.belongsTo)
        assertEquals(image.id, manager.getLoadedImageDataEntry(image.id)?.belongsTo)
    }
}
