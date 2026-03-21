package plutoproject.feature.gallery.core.display

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.dummyUuid
import plutoproject.feature.gallery.core.sampleDisplayInstance

class DisplayInstanceResourcesTest {
    @Test
    fun `should acquire loaded instances and ignore missing ids in batch`() {
        val resources = DisplayInstanceResources()
        val first = sampleDisplayInstance(id = dummyUuid(8001), belongsTo = dummyUuid(8002), chunkX = 1, chunkZ = 2)
        val second = sampleDisplayInstance(id = dummyUuid(8003), belongsTo = first.belongsTo, chunkX = 1, chunkZ = 2)

        resources.put(first.id, first)
        resources.put(second.id, second)

        val batch = resources.acquireBatch(listOf(first.id, second.id, dummyUuid(8004)))

        assertEquals(setOf(first.id, second.id), batch.keys)
        batch.values.forEach { it.close() }
    }

    @Test
    fun `should dispose removed entry and remove it from entries`() {
        val resources = DisplayInstanceResources()
        val display = sampleDisplayInstance(id = dummyUuid(8010), belongsTo = dummyUuid(8011))

        resources.put(display.id, display)
        val handle = resources.acquire(display.id)

        resources.remove(display.id)

        assertNull(resources.acquire(display.id))
        assertFalse(resources.entries.containsKey(display.id))
        assertThrows(IllegalStateException::class.java) { handle!!.value }
    }

    @Test
    fun `should dispose all entries and reject later operations`() {
        val resources = DisplayInstanceResources()
        val first = sampleDisplayInstance(id = dummyUuid(8020), belongsTo = dummyUuid(8022))
        val second = sampleDisplayInstance(id = dummyUuid(8021), belongsTo = dummyUuid(8022))

        resources.putAll(mapOf(first.id to first, second.id to second))
        val handles = resources.acquireBatch(listOf(first.id, second.id)).values.toList()

        resources.dispose()

        assertTrue(resources.entries.isEmpty())
        handles.forEach { handle ->
            assertThrows(IllegalStateException::class.java) { handle.value }
        }
        assertThrows(IllegalStateException::class.java) { resources.put(first.id, first) }
        assertThrows(IllegalStateException::class.java) { resources.acquire(first.id) }
    }
}
