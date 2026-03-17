package plutoproject.feature.gallery.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDisplaySchedulerTest {
    @Test
    fun `schedule should start loop and wake due job`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scheduler = createScheduler(dispatcher)
        val job = FakeDisplayJob()

        scheduler.scheduleAwakeAt(job, Instant.now())
        advanceUntilIdle()

        assertEquals(1, job.wakeCount)
        assertEquals(SchedulerState.IDLING, scheduler.state)
    }

    @Test
    fun `unschedule should move scheduler back to idling when last job is removed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scheduler = createScheduler(dispatcher)
        val job = FakeDisplayJob()

        scheduler.scheduleAwakeAt(job, Instant.now().plusSeconds(60))
        scheduler.unschedule(job)
        advanceUntilIdle()

        assertEquals(SchedulerState.IDLING, scheduler.state)
        assertEquals(0, job.wakeCount)
    }

    @Test
    fun `schedule should throw after stop`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scheduler = createScheduler(dispatcher)

        scheduler.stop()

        assertThrows(IllegalStateException::class.java) {
            scheduler.scheduleAwakeAt(FakeDisplayJob(), Instant.now())
        }
    }

    @Test
    fun `external scope cancellation should stop scheduler`() = runTest {
        val parentJob = Job()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(parentJob + dispatcher)
        val scheduler = DefaultDisplayScheduler(
            coroutineScope = scope,
            schedulerContext = dispatcher,
            awakeContext = dispatcher,
        )
        val job = FakeDisplayJob()

        scheduler.scheduleAwakeAt(job, Instant.now().plusSeconds(60))
        parentJob.cancel()
        advanceUntilIdle()

        assertEquals(SchedulerState.STOPPED, scheduler.state)
        assertThrows(IllegalStateException::class.java) {
            scheduler.scheduleAwakeAt(FakeDisplayJob(), Instant.now())
        }
    }

    private fun createScheduler(dispatcher: TestDispatcher): DefaultDisplayScheduler {
        return DefaultDisplayScheduler(
            coroutineScope = CoroutineScope(dispatcher),
            schedulerContext = dispatcher,
            awakeContext = dispatcher,
        )
    }

    private class FakeDisplayJob : DisplayJob {
        override val managedDisplayInstances: Map<UUID, DisplayInstance> = emptyMap()

        var wakeCount: Int = 0
            private set

        override fun wake() {
            wakeCount += 1
        }

        override fun cleanup() = Unit
    }
}
