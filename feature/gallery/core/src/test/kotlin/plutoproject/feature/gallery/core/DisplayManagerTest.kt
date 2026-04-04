package plutoproject.feature.gallery.core

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.display.DisplayManager
import plutoproject.feature.gallery.core.display.SchedulerState
import plutoproject.feature.gallery.core.display.job.StaticDisplayJob

class DisplayManagerTest {
    @Test
    fun `should create get find and delete instances through repository backed cache`() = runTest {
        val repo = InMemoryDisplayInstanceRepository()
        val runtime = newDisplayRuntime(this, instances = repo)
        val manager = runtime.manager
        val first = sampleDisplayInstance(id = dummyUuid(4002), imageId = dummyUuid(4001), chunkX = 10, chunkZ = 20)
        val second = sampleDisplayInstance(id = dummyUuid(4003), imageId = first.imageId, chunkX = 10, chunkZ = 20)

        try {
            assertEquals(DisplayManager.CreateInstanceResult.Success(first), manager.createInstance(first))
            assertEquals(DisplayManager.CreateInstanceResult.Success(second), manager.createInstance(second))
            assertEquals(DisplayManager.CreateInstanceResult.AlreadyExists(first), manager.createInstance(first))

            assertSame(first, manager.getInstance(first.id))
            assertEquals(setOf(first.id, second.id), manager.findInstanceByImageId(first.imageId).map { it.id }.toSet())
            assertEquals(setOf(first.id, second.id), manager.findInstanceByChunk(10, 20).map { it.id }.toSet())

            val deleted = manager.deleteInstance(first.id)
            assertEquals(DisplayManager.DeleteInstanceResult.Success(first), deleted)
            assertNull(manager.getInstance(first.id))
            assertEquals(DisplayManager.DeleteInstanceResult.NotFound, manager.deleteInstance(first.id))
        } finally {
            runtime.manager.close()
            runtime.scheduler.stop()
        }
    }

    @Test
    fun `should create start and stop display jobs without attach orchestration`() = runTest {
        val runtime = newDisplayRuntime(this, clock = schedulerClock(this))
        val manager = runtime.manager
        val scheduler = runtime.scheduler
        val image = sampleImage(id = dummyUuid(4010))
        val entry = sampleStaticImageDataEntry(image.id)

        try {
            val created = manager.createDisplayJob(image, entry)
            val job = (created as DisplayManager.CreateDisplayJobResult.Success).job

            assertTrue(job is StaticDisplayJob)
            assertSame(job, manager.getDisplayJob(image.id))
            assertEquals(DisplayManager.CreateDisplayJobResult.AlreadyExists(job), manager.createDisplayJob(image, entry))

            manager.startDisplayJob(job)
            advanceUntilIdle()

            assertEquals(SchedulerState.RUNNING, scheduler.state)
            assertEquals(DisplayManager.StopDisplayJobResult.Success(job), manager.stopDisplayJob(image.id))
            assertNull(manager.getDisplayJob(image.id))
            assertEquals(DisplayManager.StopDisplayJobResult.NotFound, manager.stopDisplayJob(image.id))
        } finally {
            runtime.manager.close()
            runtime.scheduler.stop()
        }
    }

    @Test
    fun `should manage send job lifecycle independently from instance cache`() = runTest {
        val runtime = newDisplayRuntime(this)
        val manager = runtime.manager
        val playerId = dummyUuid(4020)

        try {
            val started = manager.startSendJob(playerId)
            val job = (started as DisplayManager.StartSendJobResult.Success).job

            assertSame(job, manager.getSendJob(playerId))
            assertEquals(DisplayManager.StartSendJobResult.AlreadyStarted(job), manager.startSendJob(playerId))
            assertEquals(DisplayManager.StopSendJobResult.Success(job), manager.stopSendJob(playerId))
            assertNull(manager.getSendJob(playerId))
            assertEquals(DisplayManager.StopSendJobResult.NotStarted, manager.stopSendJob(playerId))
        } finally {
            runtime.manager.close()
            runtime.scheduler.stop()
        }
    }

}
