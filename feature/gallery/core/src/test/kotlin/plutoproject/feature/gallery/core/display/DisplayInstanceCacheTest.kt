package plutoproject.feature.gallery.core.display

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.dummyUuid
import plutoproject.feature.gallery.core.sampleDisplayInstance
import plutoproject.feature.gallery.core.util.ChunkKey
import plutoproject.feature.gallery.core.util.ResourceCache
import plutoproject.feature.gallery.core.util.ResourceCacheTest
import java.util.UUID

class DisplayInstanceCacheTest : ResourceCacheTest<UUID, DisplayInstance>() {
    override fun createCache(): ResourceCache<UUID, DisplayInstance, *> = DisplayInstanceCache()

    override fun createValue(seed: Long): DisplayInstance {
        return sampleDisplayInstance(
            id = dummyUuid(seed),
            belongsTo = dummyUuid(seed + 10_000),
            chunkX = seed.toInt(),
            chunkZ = (seed + 1).toInt()
        )
    }

    override fun keyOf(value: DisplayInstance): UUID = value.id

    @Test
    fun `should acquire instances by belongsTo index`() {
        val resources = DisplayInstanceCache()
        val belongsTo = dummyUuid(8012)
        val first = sampleDisplayInstance(id = dummyUuid(8013), belongsTo = belongsTo, chunkX = 4, chunkZ = 5)
        val second = sampleDisplayInstance(id = dummyUuid(8014), belongsTo = belongsTo, chunkX = 4, chunkZ = 5)
        val third = sampleDisplayInstance(id = dummyUuid(8015), belongsTo = dummyUuid(8016), chunkX = 8, chunkZ = 9)

        resources.putAll(
            mapOf(
                first.id to first,
                second.id to second,
                third.id to third
            )
        )

        val belongsToHandles = resources.acquireByBelongsTo(belongsTo)
        assertEquals(setOf(first.id, second.id), belongsToHandles.map { it.value.id }.toSet())
        belongsToHandles.forEach { it.close() }
    }

    @Test
    fun `should acquire instances by chunk index`() {
        val resources = DisplayInstanceCache()
        val first = sampleDisplayInstance(id = dummyUuid(8023), belongsTo = dummyUuid(8024), chunkX = 4, chunkZ = 5)
        val second = sampleDisplayInstance(id = dummyUuid(8025), belongsTo = dummyUuid(8026), chunkX = 4, chunkZ = 5)
        val third = sampleDisplayInstance(id = dummyUuid(8027), belongsTo = dummyUuid(8028), chunkX = 8, chunkZ = 9)

        resources.putAll(
            mapOf(
                first.id to first,
                second.id to second,
                third.id to third
            )
        )

        val chunkHandles = resources.acquireByChunk(ChunkKey(4, 5))
        assertEquals(setOf(first.id, second.id), chunkHandles.map { it.value.id }.toSet())
        chunkHandles.forEach { it.close() }
    }

    @Test
    fun `should clear indexes when removing instance`() {
        val resources = DisplayInstanceCache()
        val belongsTo = dummyUuid(8017)
        val display = sampleDisplayInstance(id = dummyUuid(8018), belongsTo = belongsTo, chunkX = 6, chunkZ = 7)

        resources.put(display.id, display)

        resources.remove(display.id)

        assertTrue(resources.acquireByBelongsTo(belongsTo).isEmpty())
        assertTrue(resources.acquireByChunk(ChunkKey(6, 7)).isEmpty())
    }
}
