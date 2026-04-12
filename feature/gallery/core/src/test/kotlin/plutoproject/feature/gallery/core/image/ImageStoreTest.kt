package plutoproject.feature.gallery.core.image

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.InMemoryImageDataRepository
import plutoproject.feature.gallery.core.InMemoryImageRepository
import plutoproject.feature.gallery.core.dummyUuid
import plutoproject.feature.gallery.core.newImageDataStore
import plutoproject.feature.gallery.core.newImageStore
import plutoproject.feature.gallery.core.sampleImage
import plutoproject.feature.gallery.core.sampleStaticImageData

class ImageStoreTest {
    @Test
    fun `image store should create get save and delete images`() = runTest {
        val store = newImageStore()
        val image = sampleImage(id = dummyUuid(10))

        assertTrue(store.create(image))
        assertFalse(store.create(image))
        assertSame(image, store.get(image.id))
        assertEquals(mapOf(image.id to image), store.getMany(listOf(image.id)))
        assertEquals(listOf(image), store.findByOwner(image.owner))
        assertEquals(1, store.count())

        val renamed = image.renamed("renamed-image")
        assertTrue(store.save(renamed))
        assertEquals("renamed-image", store.get(image.id)?.name)

        val deleted = store.delete(image.id)
        assertEquals("renamed-image", deleted?.name)
        assertNull(store.get(image.id))
        assertFalse(store.save(image))
    }

    @Test
    fun `image store should preserve batch snapshot without fallback lookups`() = runTest {
        val repo = object : InMemoryImageRepository() {
            var findByIdsCalls = 0

            override suspend fun findByIds(ids: Collection<java.util.UUID>): Map<java.util.UUID, Image> {
                findByIdsCalls += 1
                return super.findByIds(ids)
            }
        }
        val store = newImageStore(repo)
        val first = sampleImage(id = dummyUuid(21))
        val second = sampleImage(id = dummyUuid(22))
        repo.save(first)
        repo.save(second)

        val result = store.getMany(listOf(first.id, second.id, first.id))

        assertEquals(setOf(first.id, second.id), result.keys)
        assertEquals(1, repo.findByIdsCalls)
    }

    @Test
    fun `image data store should create get save and delete data snapshots`() = runTest {
        val store = newImageDataStore()
        val imageId = dummyUuid(30)
        val data = sampleStaticImageData()

        assertTrue(store.create(imageId, data))
        assertFalse(store.create(imageId, data))
        assertSame(data, store.get(imageId))
        assertEquals(mapOf(imageId to data), store.getMany(listOf(imageId)))

        val newData = sampleStaticImageData()
        assertTrue(store.save(imageId, newData))
        assertSame(newData, store.get(imageId))

        val deleted = store.delete(imageId)
        assertSame(newData, deleted)
        assertNull(store.get(imageId))
        assertFalse(store.save(imageId, data))
    }

    @Test
    fun `image should return new immutable snapshots when renamed or owner name changes`() {
        val image = sampleImage(id = dummyUuid(40))

        val renamed = image.renamed("new-name")
        val ownerChanged = image.withOwnerName("Owner_456")

        assertEquals("test-image", image.name)
        assertEquals("Owner_123", image.ownerName)
        assertEquals("new-name", renamed.name)
        assertEquals("Owner_456", ownerChanged.ownerName)
    }
}
