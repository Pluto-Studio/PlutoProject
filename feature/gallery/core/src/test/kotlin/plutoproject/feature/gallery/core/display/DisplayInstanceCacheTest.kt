package plutoproject.feature.gallery.core.display

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.RESOURCE_CACHE_TTL_SECONDS
import plutoproject.feature.gallery.core.dummyUuid
import plutoproject.feature.gallery.core.sampleDisplayInstance
import plutoproject.feature.gallery.core.util.ChunkKey
import plutoproject.feature.gallery.core.util.ResourceCache
import plutoproject.feature.gallery.core.util.ResourceCacheTest
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class DisplayInstanceCacheTest : ResourceCacheTest<UUID, DisplayInstance>() {
    override fun createCache(): ResourceCache<UUID, DisplayInstance, *> = createDisplayInstanceCache()

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
        val resources = createDisplayInstanceCache()
        val belongsTo = dummyUuid(8012)
        val first = sampleDisplayInstance(id = dummyUuid(8013), belongsTo = belongsTo, chunkX = 4, chunkZ = 5)
        val second = sampleDisplayInstance(id = dummyUuid(8014), belongsTo = belongsTo, chunkX = 4, chunkZ = 5)
        val third = sampleDisplayInstance(id = dummyUuid(8015), belongsTo = dummyUuid(8016), chunkX = 8, chunkZ = 9)

        resources.putAll(mapOf(first.id to first, second.id to second, third.id to third))

        val belongsToHandles = resources.acquireByBelongsTo(belongsTo)
        assertEquals(setOf(first.id, second.id), belongsToHandles.map { it.value.id }.toSet())
        belongsToHandles.forEach { it.close() }
    }

    @Test
    fun `should acquire instances by chunk index`() {
        val resources = createDisplayInstanceCache()
        val first = sampleDisplayInstance(id = dummyUuid(8023), belongsTo = dummyUuid(8024), chunkX = 4, chunkZ = 5)
        val second = sampleDisplayInstance(id = dummyUuid(8025), belongsTo = dummyUuid(8026), chunkX = 4, chunkZ = 5)
        val third = sampleDisplayInstance(id = dummyUuid(8027), belongsTo = dummyUuid(8028), chunkX = 8, chunkZ = 9)

        resources.putAll(mapOf(first.id to first, second.id to second, third.id to third))

        val chunkHandles = resources.acquireByChunk(ChunkKey(4, 5))
        assertEquals(setOf(first.id, second.id), chunkHandles.map { it.value.id }.toSet())
        chunkHandles.forEach { it.close() }
    }

    @Test
    fun `should clear indexes when removing instance`() {
        val resources = createDisplayInstanceCache()
        val belongsTo = dummyUuid(8017)
        val display = sampleDisplayInstance(id = dummyUuid(8018), belongsTo = belongsTo, chunkX = 6, chunkZ = 7)

        resources.put(display.id, display)

        resources.remove(display.id)

        assertTrue(resources.acquireByBelongsTo(belongsTo).isEmpty())
        assertTrue(resources.acquireByChunk(ChunkKey(6, 7)).isEmpty())
    }

    @Test
    fun `should auto remove expired instance and clear indexes`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val resources = createDisplayInstanceCache(this, dispatcher, schedulerClock(this))
        val display = sampleDisplayInstance(
            id = dummyUuid(9001),
            belongsTo = dummyUuid(9002),
            chunkX = 10,
            chunkZ = 11,
        )

        resources.put(display.id, display)

        advanceTimeBy(RESOURCE_CACHE_TTL_SECONDS * 1000)
        advanceUntilIdle()

        assertFalse(resources.entries.containsKey(display.id))
        assertNull(resources.acquire(display.id))
        assertTrue(resources.acquireByBelongsTo(display.belongsTo).isEmpty())
        assertTrue(resources.acquireByChunk(ChunkKey(display.chunkX, display.chunkZ)).isEmpty())
    }

    @Test
    fun `should reschedule cleanup after entry survives ttl while acquired`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val resources = createDisplayInstanceCache(this, dispatcher, schedulerClock(this))
        val display = sampleDisplayInstance(
            id = dummyUuid(9011),
            belongsTo = dummyUuid(9012),
            chunkX = 12,
            chunkZ = 13,
        )

        resources.put(display.id, display)
        val handle = resources.acquire(display.id)!!

        advanceTimeBy(RESOURCE_CACHE_TTL_SECONDS * 1000)
        advanceUntilIdle()

        assertTrue(resources.entries.containsKey(display.id))

        handle.close()
        runCurrent()

        assertTrue(resources.entries.containsKey(display.id))

        advanceTimeBy(RESOURCE_CACHE_TTL_SECONDS * 1000)
        advanceUntilIdle()

        assertFalse(resources.entries.containsKey(display.id))
        assertNull(resources.acquire(display.id))
    }

    private fun createDisplayInstanceCache(): DisplayInstanceCache = DisplayInstanceCache(
        coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        coroutineContext = Dispatchers.Unconfined,
        clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)
    )

    private fun createDisplayInstanceCache(
        scope: TestScope,
        dispatcher: CoroutineContext,
        clock: Clock,
    ): DisplayInstanceCache = DisplayInstanceCache(
        coroutineScope = scope,
        coroutineContext = dispatcher,
        clock = clock,
    )

    private fun schedulerClock(scope: TestScope): Clock {
        return object : Clock() {
            override fun getZone() = ZoneOffset.UTC

            override fun withZone(zone: ZoneId?): Clock = this

            override fun instant(): Instant = Instant.ofEpochMilli(scope.testScheduler.currentTime)
        }
    }
}
