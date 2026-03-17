package plutoproject.feature.gallery.core

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DisplayManagerTest {
    @Test
    fun `should manage display lifecycle with belongs and chunk indexes`() = runTest {
        val manager = DisplayManager()
        val belongsTo = dummyUuid(4001)
        val first = sampleDisplayInstance(id = dummyUuid(4002), belongsTo = belongsTo, chunkX = 10, chunkZ = 20)
        val second = sampleDisplayInstance(id = dummyUuid(4003), belongsTo = belongsTo, chunkX = 10, chunkZ = 20)

        manager.loadDisplayInstance(first)
        manager.loadDisplayInstance(second)

        assertNotNull(manager.getLoadedDisplayInstance(first.id))
        assertEquals(2, manager.getLoadedDisplayInstancesByBelongsTo(belongsTo).size)
        assertEquals(2, manager.getLoadedDisplayInstancesByChunk(10, 20).size)

        manager.unloadDisplayInstance(first.id)
        assertNull(manager.getLoadedDisplayInstance(first.id))
        assertEquals(1, manager.getLoadedDisplayInstancesByBelongsTo(belongsTo).size)
        assertEquals(1, manager.getLoadedDisplayInstancesByChunk(10, 20).size)
    }

    @Test
    fun `should load through lifecycle getters for get and lookup`() = runTest {
        val manager = DisplayManager()
        val display = sampleDisplayInstance(id = dummyUuid(4004), belongsTo = dummyUuid(4005), chunkX = 99, chunkZ = 100)

        val loaded = manager.getDisplayInstance(display.id) { display }
        assertEquals(display.id, loaded?.id)

        val byBelongs = manager.lookupDisplayInstancesByBelongsTo(display.belongsTo) { listOf(display) }
        assertEquals(1, byBelongs.size)

        val byChunk = manager.lookupDisplayInstancesByChunk(display.chunkX, display.chunkZ) { _, _ -> listOf(display) }
        assertEquals(1, byChunk.size)
    }
}
