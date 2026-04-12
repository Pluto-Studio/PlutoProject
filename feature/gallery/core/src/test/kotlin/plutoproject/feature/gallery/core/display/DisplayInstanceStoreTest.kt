package plutoproject.feature.gallery.core.display

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.InMemoryDisplayInstanceRepository
import plutoproject.feature.gallery.core.dummyUuid
import plutoproject.feature.gallery.core.sampleDisplayInstance

class DisplayInstanceStoreTest {
    @Test
    fun `display instance store should delegate batch lookup to repository`() = runTest {
        val repo = InMemoryDisplayInstanceRepository()
        val store = DisplayInstanceStore(repo)
        val first = sampleDisplayInstance(id = dummyUuid(7001))
        val second = sampleDisplayInstance(id = dummyUuid(7002))
        repo.save(first)
        repo.save(second)

        val result = store.getMany(listOf(first.id, second.id, first.id, dummyUuid(7999)))

        assertEquals(mapOf(first.id to first, second.id to second), result)
    }
}
