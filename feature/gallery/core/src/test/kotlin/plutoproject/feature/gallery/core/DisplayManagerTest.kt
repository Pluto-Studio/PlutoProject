package plutoproject.feature.gallery.core

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

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

    @Test
    fun `should support batch display instance cache helpers`() {
        val manager = DisplayManager()
        val first = sampleDisplayInstance(id = dummyUuid(4006), belongsTo = dummyUuid(4007), chunkX = 1, chunkZ = 2)
        val second = sampleDisplayInstance(id = dummyUuid(4008), belongsTo = dummyUuid(4007), chunkX = 1, chunkZ = 2)

        manager.loadDisplayInstances(listOf(first, second))

        assertEquals(setOf(first.id, second.id), manager.getLoadedDisplayInstances(listOf(first.id, second.id, dummyUuid(4009))).keys)
        assertEquals(2, manager.unloadDisplayInstances(listOf(first.id, second.id)).size)
        assertTrue(manager.getLoadedDisplayInstances(listOf(first.id, second.id)).isEmpty())
    }

    @Test
    fun `should manage display job registry and display instance job bindings`() {
        val manager = DisplayManager()
        val belongsTo = dummyUuid(4010)
        val job = FakeDisplayJob(belongsTo = belongsTo)
        val firstDisplayId = dummyUuid(4011)
        val secondDisplayId = dummyUuid(4012)

        assertSame(job, manager.registerDisplayJob(job))
        assertSame(job, manager.getLoadedDisplayJob(belongsTo))
        assertEquals(listOf(job), manager.getLoadedDisplayJobs())

        manager.bindDisplayInstanceToJob(firstDisplayId, belongsTo)
        manager.bindDisplayInstanceToJob(secondDisplayId, belongsTo)

        assertEquals(belongsTo, manager.getJobBelongsToByDisplayInstanceId(firstDisplayId))
        assertEquals(belongsTo, manager.getJobBelongsToByDisplayInstanceId(secondDisplayId))

        assertEquals(belongsTo, manager.unbindDisplayInstanceFromJob(firstDisplayId))
        assertNull(manager.getJobBelongsToByDisplayInstanceId(firstDisplayId))
        assertEquals(belongsTo, manager.getJobBelongsToByDisplayInstanceId(secondDisplayId))

        assertSame(job, manager.removeDisplayJob(belongsTo))
        assertNull(manager.getLoadedDisplayJob(belongsTo))
        assertTrue(manager.getLoadedDisplayJobs().isEmpty())
    }

    @Test
    fun `should manage send job registry independently from display caches`() {
        val manager = DisplayManager()
        val sendJob = FakeSendJob(playerId = dummyUuid(4020))
        val display = sampleDisplayInstance(id = dummyUuid(4021), belongsTo = dummyUuid(4022), chunkX = 7, chunkZ = 8)

        manager.loadDisplayInstance(display)
        assertSame(sendJob, manager.registerSendJob(sendJob))

        assertSame(sendJob, manager.getLoadedSendJob(sendJob.playerId))
        assertEquals(listOf(sendJob), manager.getLoadedSendJobs())
        assertNotNull(manager.getLoadedDisplayInstance(display.id))

        assertSame(sendJob, manager.removeSendJob(sendJob.playerId))
        assertNull(manager.getLoadedSendJob(sendJob.playerId))
        assertTrue(manager.getLoadedSendJobs().isEmpty())
        assertNotNull(manager.getLoadedDisplayInstance(display.id))
    }

    private class FakeDisplayJob(
        override val belongsTo: UUID,
    ) : DisplayJob {
        override val isStopped: Boolean = false
        override val managedDisplayInstances: Map<UUID, DisplayInstance> = emptyMap()

        override fun attach(
            displayInstance: DisplayInstance,
            image: Image,
            imageDataEntry: ImageDataEntry<*>,
        ) = Unit

        override fun detach(displayInstanceId: UUID): DisplayInstance? = null

        override fun isEmpty(): Boolean = true

        override fun wake() = Unit

        override fun stop() = Unit
    }

    private class FakeSendJob(
        override val playerId: UUID,
    ) : SendJob {
        override val state: SendJobState = SendJobState.IDLING

        override fun enqueue(update: MapUpdate) = Unit

        override fun stop() = Unit
    }
}
