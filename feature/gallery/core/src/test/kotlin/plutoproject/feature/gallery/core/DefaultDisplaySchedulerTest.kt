package plutoproject.feature.gallery.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDisplaySchedulerTest {
    @Test
    fun `schedule should start loop and wake due job`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = SchedulerTestClock(testScheduler)
        val scheduler = createScheduler(dispatcher, clock)
        val job = FakeDisplayJob()

        scheduler.scheduleAwakeAt(job, clock.instant())
        advanceUntilIdle()

        assertEquals(1, job.wakeCount)
        assertEquals(SchedulerState.IDLING, scheduler.state)
    }

    @Test
    fun `unschedule should move scheduler back to idling when last job is removed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = SchedulerTestClock(testScheduler)
        val scheduler = createScheduler(dispatcher, clock)
        val job = FakeDisplayJob()

        scheduler.scheduleAwakeAt(job, clock.instant().plusSeconds(60))
        scheduler.unschedule(job)
        advanceUntilIdle()

        assertEquals(SchedulerState.IDLING, scheduler.state)
        assertEquals(0, job.wakeCount)
    }

    @Test
    fun `schedule should throw after stop`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = SchedulerTestClock(testScheduler)
        val scheduler = createScheduler(dispatcher, clock)

        scheduler.stop()

        assertThrows(IllegalStateException::class.java) {
            scheduler.scheduleAwakeAt(FakeDisplayJob(), clock.instant())
        }
    }

    @Test
    fun `external scope cancellation should stop scheduler`() = runTest {
        val parentJob = Job()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val clock = SchedulerTestClock(testScheduler)
        val scope = CoroutineScope(parentJob + dispatcher)
        val scheduler = DefaultDisplayScheduler(
            clock = clock,
            coroutineScope = scope,
            schedulerContext = dispatcher,
            awakeContext = dispatcher,
        )
        val job = FakeDisplayJob()

        scheduler.scheduleAwakeAt(job, clock.instant().plusSeconds(60))
        parentJob.cancel()
        advanceUntilIdle()

        assertEquals(SchedulerState.STOPPED, scheduler.state)
        assertThrows(IllegalStateException::class.java) {
            scheduler.scheduleAwakeAt(FakeDisplayJob(), clock.instant())
        }
    }

    private fun createScheduler(dispatcher: TestDispatcher, clock: Clock): DefaultDisplayScheduler {
        return DefaultDisplayScheduler(
            clock = clock,
            coroutineScope = CoroutineScope(dispatcher),
            schedulerContext = dispatcher,
            awakeContext = dispatcher,
        )
    }

    private class SchedulerTestClock(
        private val scheduler: TestCoroutineScheduler,
        private val zoneId: ZoneId = ZoneId.of("UTC"),
        private val baseInstant: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    ) : Clock() {
        override fun instant(): Instant = baseInstant.plusMillis(scheduler.currentTime)

        override fun getZone(): ZoneId = zoneId

        override fun withZone(zone: ZoneId): Clock = SchedulerTestClock(scheduler, zone, baseInstant)
    }

    private class FakeDisplayJob : DisplayJob {
        override var isStopped: Boolean = false
        override val managedDisplayInstances: Map<UUID, DisplayInstance> = emptyMap()

        var wakeCount: Int = 0
            private set

        override fun wake() {
            wakeCount += 1
        }

        override fun stop() {
            isStopped = true
        }
    }
}
