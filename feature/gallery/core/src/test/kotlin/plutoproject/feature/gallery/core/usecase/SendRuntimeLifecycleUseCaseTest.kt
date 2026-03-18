package plutoproject.feature.gallery.core.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.DisplayManager
import plutoproject.feature.gallery.core.MapUpdate
import plutoproject.feature.gallery.core.SendJob
import plutoproject.feature.gallery.core.SendJobFactory
import plutoproject.feature.gallery.core.SendJobState
import plutoproject.feature.gallery.core.dummyUuid
import java.util.UUID

class SendRuntimeLifecycleUseCaseTest {
    @Test
    fun `start send job should create and register unique player job`() {
        val manager = DisplayManager()
        val job = FakeSendJob(dummyUuid(7101))
        val factory = RecordingSendJobFactory(job)
        val useCase = StartSendJobUseCase(manager, factory)

        val result = useCase.execute(job.playerId)

        assertEquals(StartSendJobUseCase.Result.Ok(job), result)
        assertSame(job, manager.getLoadedSendJob(job.playerId))
        assertEquals(job.playerId, factory.lastPlayerId)
    }

    @Test
    fun `start send job should return already started when same player job exists`() {
        val manager = DisplayManager()
        val existed = FakeSendJob(dummyUuid(7111))
        manager.registerSendJob(existed)

        val result = StartSendJobUseCase(manager, RecordingSendJobFactory(FakeSendJob(dummyUuid(7112)))).execute(existed.playerId)

        assertEquals(StartSendJobUseCase.Result.AlreadyStarted(existed), result)
    }

    @Test
    fun `stop send job should stop and remove runtime job`() {
        val manager = DisplayManager()
        val job = FakeSendJob(dummyUuid(7121))
        manager.registerSendJob(job)

        val result = StopSendJobUseCase(manager).execute(job.playerId)

        assertEquals(StopSendJobUseCase.Result.Ok(job), result)
        assertTrue(job.stopCalled)
        assertNull(manager.getLoadedSendJob(job.playerId))
    }

    @Test
    fun `stop send job should return not started when absent`() {
        assertEquals(
            StopSendJobUseCase.Result.NotStarted,
            StopSendJobUseCase(DisplayManager()).execute(dummyUuid(7131))
        )
    }

    private class RecordingSendJobFactory(
        private val job: SendJob,
    ) : SendJobFactory {
        var lastPlayerId: UUID? = null

        override fun create(playerId: UUID): SendJob {
            lastPlayerId = playerId
            return job
        }
    }

    private class FakeSendJob(
        override val playerId: UUID,
    ) : SendJob {
        override val state: SendJobState = SendJobState.IDLING
        var stopCalled: Boolean = false

        override fun enqueue(update: MapUpdate) = Unit

        override fun stop() {
            stopCalled = true
        }
    }
}
