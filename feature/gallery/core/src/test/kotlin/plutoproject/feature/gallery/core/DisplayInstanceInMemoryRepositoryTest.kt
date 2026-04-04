package plutoproject.feature.gallery.core

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DisplayInstanceInMemoryRepositoryTest {
    @Test
    fun `in memory repository should support find lookup and delete`() = runTest {
        val repo = InMemoryDisplayInstanceRepository()
        val imageId = dummyUuid(5001)
        val first = sampleDisplayInstance(id = dummyUuid(5002), imageId = imageId, chunkX = 1, chunkZ = 2)
        val second = sampleDisplayInstance(id = dummyUuid(5003), imageId = imageId, chunkX = 1, chunkZ = 2)
        val third = sampleDisplayInstance(id = dummyUuid(5004), imageId = dummyUuid(5005), chunkX = 9, chunkZ = 9)

        repo.save(first)
        repo.save(second)
        repo.save(third)

        assertNotNull(repo.findById(first.id))
        assertEquals(2, repo.findByImageId(imageId).size)
        assertEquals(2, repo.findByChunk(1, 2).size)

        repo.deleteById(first.id)
        assertNull(repo.findById(first.id))
        assertNotNull(repo.findById(second.id))
    }
}
