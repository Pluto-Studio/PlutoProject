package plutoproject.feature.gallery.core.display.usecase

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.display.DisplayManager
import plutoproject.feature.gallery.core.display.MapUpdate
import plutoproject.feature.gallery.core.display.MapUpdatePort
import plutoproject.feature.gallery.core.display.job.SendJob
import plutoproject.feature.gallery.core.display.job.SendJobFactory
import plutoproject.feature.gallery.core.display.job.SendJobState
import plutoproject.feature.gallery.core.dummyUuid
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class SendRuntimeLifecycleUseCaseTest {
    @Test
    fun `start send job should create and register unique player job`() = runTest {
        val manager = DisplayManager()
        val factory = newSendJobFactory(this)
        val useCase = StartSendJobUseCase(manager, factory)
        val playerId = dummyUuid(7101)

        val result = useCase.execute(playerId)
        val job = (result as StartSendJobUseCase.Result.Ok).job

        assertSame(job, manager.getLoadedSendJob(playerId))
        assertEquals(playerId, job.playerId)
        assertEquals(SendJobState.IDLING, job.state)
    }

    @Test
    fun `start send job should return already started when same player job exists`() = runTest {
        val manager = DisplayManager()
        val existed = newSendJob(this, dummyUuid(7111))
        manager.registerSendJob(existed)

        val result = StartSendJobUseCase(manager, newSendJobFactory(this)).execute(existed.playerId)

        assertEquals(StartSendJobUseCase.Result.AlreadyStarted(existed), result)
    }

    @Test
    fun `stop send job should stop and remove runtime job`() = runTest {
        val manager = DisplayManager()
        val job = newSendJob(this, dummyUuid(7121))
        manager.registerSendJob(job)

        val result = StopSendJobUseCase(manager).execute(job.playerId)

        assertEquals(StopSendJobUseCase.Result.Ok(job), result)
        assertEquals(SendJobState.STOPPED, job.state)
        assertNull(manager.getLoadedSendJob(job.playerId))
    }

    @Test
    fun `stop send job should return not started when absent`() {
        assertEquals(
            StopSendJobUseCase.Result.NotStarted,
            StopSendJobUseCase(DisplayManager()).execute(dummyUuid(7131))
        )
    }

    private fun newSendJobFactory(scope: TestScope): SendJobFactory {
        return SendJobFactory(
            clock = schedulerClock(scope),
            coroutineScope = scope,
            loopContext = StandardTestDispatcher(scope.testScheduler),
            mapUpdatePort = object : MapUpdatePort {
                override fun send(playerId: UUID, update: MapUpdate) = Unit
            },
            maxQueueSize = 8,
            maxUpdatesInSpan = 2,
            updateLimitSpan = 1.seconds,
        )
    }

    private fun newSendJob(scope: TestScope, playerId: UUID): SendJob {
        return SendJob(
            playerId = playerId,
            maxQueueSize = 8,
            maxUpdatesInSpan = 2,
            updateLimitSpan = 1.seconds,
            clock = schedulerClock(scope),
            coroutineScope = scope,
            loopContext = StandardTestDispatcher(scope.testScheduler),
            mapUpdatePort = object : MapUpdatePort {
                override fun send(playerId: UUID, update: MapUpdate) = Unit
            },
        )
    }

    private fun schedulerClock(scope: TestScope): Clock {
        return object : Clock() {
            override fun getZone(): ZoneId = ZoneOffset.UTC

            override fun withZone(zone: ZoneId?): Clock = this

            override fun instant(): Instant {
                return Instant.ofEpochMilli(scope.testScheduler.currentTime)
            }
        }
    }
}
