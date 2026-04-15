package plutoproject.feature.gallery.core

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.display.MapUpdate
import plutoproject.feature.gallery.core.display.MapUpdatePort
import plutoproject.feature.gallery.core.display.job.MapContentFingerprint
import plutoproject.feature.gallery.core.display.job.SendJob
import plutoproject.feature.gallery.core.display.job.SendPriority
import plutoproject.feature.gallery.core.display.job.SendRequest
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SendJobTest {
    @Test
    fun `should send static before animated and round robin within each priority`() = runTest {
        val sent = mutableListOf<Int>()
        val job = newSendJob(
            scope = this,
            mapUpdatePort = RecordingMapUpdatePort(onSend = { _, update ->
                sent += update.mapId
            })
        )
        try {
            job.enqueue(sampleRequest(sourceId = dummyUuid(101), priority = SendPriority.ANIMATED, mapId = 31))
            job.enqueue(sampleRequest(sourceId = dummyUuid(102), priority = SendPriority.ANIMATED, mapId = 41))
            job.enqueue(sampleRequest(sourceId = dummyUuid(1), priority = SendPriority.STATIC, mapId = 11))
            job.enqueue(sampleRequest(sourceId = dummyUuid(1), priority = SendPriority.STATIC, mapId = 12))
            job.enqueue(sampleRequest(sourceId = dummyUuid(2), priority = SendPriority.STATIC, mapId = 21))
            job.enqueue(sampleRequest(sourceId = dummyUuid(2), priority = SendPriority.STATIC, mapId = 22))
            job.enqueue(sampleRequest(sourceId = dummyUuid(101), priority = SendPriority.ANIMATED, mapId = 32))
            job.enqueue(sampleRequest(sourceId = dummyUuid(102), priority = SendPriority.ANIMATED, mapId = 42))

            advanceUntilIdle()

            assertEquals(listOf(11, 21, 12, 22, 31, 41, 32, 42), sent)
        } finally {
            stopJob(job)
        }
    }

    @Test
    fun `should replace pending update with latest content for same map id`() = runTest {
        val sent = mutableListOf<MapUpdate>()
        val job = newSendJob(
            scope = this,
            mapUpdatePort = RecordingMapUpdatePort(onSend = { _, update ->
                sent += update
            })
        )

        val original = sampleRequest(sourceId = dummyUuid(1), priority = SendPriority.STATIC, mapId = 10, token = 1)
        val latest = sampleRequest(sourceId = dummyUuid(1), priority = SendPriority.STATIC, mapId = 10, token = 2)
        try {
            job.enqueue(original)
            job.enqueue(latest)

            advanceUntilIdle()

            assertEquals(listOf(latest.update), sent)
        } finally {
            stopJob(job)
        }
    }

    @Test
    fun `should skip enqueue when content matches last sent fingerprint`() = runTest {
        val sent = mutableListOf<MapUpdate>()
        val job = newSendJob(
            scope = this,
            mapUpdatePort = RecordingMapUpdatePort(onSend = { _, update ->
                sent += update
            })
        )

        val request = sampleRequest(sourceId = dummyUuid(1), priority = SendPriority.STATIC, mapId = 10, token = 5)
        try {
            job.enqueue(request)
            advanceUntilIdle()

            job.enqueue(request)
            advanceUntilIdle()

            assertEquals(listOf(request.update), sent)
        } finally {
            stopJob(job)
        }
    }

    @Test
    fun `should keep throttling window when new updates arrive during cooldown`() = runTest {
        val sendTimes = mutableListOf<Long>()
        val job = newSendJob(
            scope = this,
            mapUpdatePort = RecordingMapUpdatePort(onSend = { _, _ ->
                sendTimes += testScheduler.currentTime
            })
        )
        try {
            job.enqueue(sampleRequest(sourceId = dummyUuid(1), priority = SendPriority.STATIC, mapId = 11))
            job.enqueue(sampleRequest(sourceId = dummyUuid(1), priority = SendPriority.STATIC, mapId = 12))
            runCurrent()

            job.enqueue(sampleRequest(sourceId = dummyUuid(2), priority = SendPriority.STATIC, mapId = 21))
            runCurrent()
            assertEquals(listOf(0L), sendTimes)

            advanceUntilIdle()

            assertEquals(listOf(0L, 50L, 100L), sendTimes)
        } finally {
            stopJob(job)
        }
    }

    @Test
    fun `should allow enqueue while paused and send after resume`() = runTest {
        val sent = mutableListOf<Int>()
        val job = newSendJob(
            scope = this,
            mapUpdatePort = RecordingMapUpdatePort(onSend = { _, update ->
                sent += update.mapId
            })
        )
        try {
            job.pause()
            job.enqueue(sampleRequest(sourceId = dummyUuid(1), priority = SendPriority.STATIC, mapId = 11))
            job.enqueue(sampleRequest(sourceId = dummyUuid(2), priority = SendPriority.STATIC, mapId = 21))

            advanceUntilIdle()
            assertEquals(emptyList<Int>(), sent)

            job.resume()
            advanceUntilIdle()

            assertEquals(listOf(11, 21), sent)
        } finally {
            stopJob(job)
        }
    }

    @Test
    fun `should evict animated updates for incoming static update when queue is full`() = runTest {
        val sent = mutableListOf<Int>()
        val job = newSendJob(
            scope = this,
            maxQueueSize = 1,
            mapUpdatePort = RecordingMapUpdatePort(onSend = { _, update ->
                sent += update.mapId
            })
        )
        try {
            job.enqueue(sampleRequest(sourceId = dummyUuid(1), priority = SendPriority.ANIMATED, mapId = 31))
            job.enqueue(sampleRequest(sourceId = dummyUuid(2), priority = SendPriority.STATIC, mapId = 11))

            advanceUntilIdle()

            assertEquals(listOf(11), sent)
        } finally {
            stopJob(job)
        }
    }

    @Test
    fun `should allow static queue to soft overflow maxQueueSize`() = runTest {
        val sent = mutableListOf<Int>()
        val job = newSendJob(
            scope = this,
            maxQueueSize = 1,
            mapUpdatePort = RecordingMapUpdatePort(onSend = { _, update ->
                sent += update.mapId
            })
        )
        try {
            job.enqueue(sampleRequest(sourceId = dummyUuid(1), priority = SendPriority.STATIC, mapId = 11))
            job.enqueue(sampleRequest(sourceId = dummyUuid(2), priority = SendPriority.STATIC, mapId = 21))

            advanceUntilIdle()

            assertEquals(listOf(11, 21), sent)
        } finally {
            stopJob(job)
        }
    }

    @Test
    fun `stop should be idempotent and reject new events after actor stops`() = runTest {
        val sent = mutableListOf<Int>()
        val job = newSendJob(
            scope = this,
            mapUpdatePort = RecordingMapUpdatePort(onSend = { _, update ->
                sent += update.mapId
            })
        )
        job.enqueue(sampleRequest(sourceId = dummyUuid(1), priority = SendPriority.STATIC, mapId = 11))
        job.stop()
        job.stop()
        advanceUntilIdle()

        assertEquals(emptyList<Int>(), sent)
        assertThrows(IllegalStateException::class.java) {
            job.enqueue(sampleRequest(sourceId = dummyUuid(1), priority = SendPriority.STATIC, mapId = 12))
        }
        assertThrows(IllegalStateException::class.java) {
            job.pause()
        }
        assertThrows(IllegalStateException::class.java) {
            job.resume()
        }
    }

    private fun newSendJob(
        scope: TestScope,
        loopContext: CoroutineContext = StandardTestDispatcher(scope.testScheduler),
        maxQueueSize: Int = 8,
        maxUpdatesInSpan: Int = 1,
        updateLimitSpan: Duration = 50.milliseconds,
        mapUpdatePort: MapUpdatePort = RecordingMapUpdatePort(),
    ): SendJob {
        return SendJob(
            playerId = dummyUuid(6001),
            maxQueueSize = maxQueueSize,
            maxUpdatesInSpan = maxUpdatesInSpan,
            updateLimitSpan = updateLimitSpan,
            clock = schedulerClock(scope),
            coroutineScope = scope,
            loopContext = loopContext,
            mapUpdatePort = mapUpdatePort,
        )
    }

    private fun TestScope.stopJob(job: SendJob) {
        job.stop()
        advanceUntilIdle()
    }

    private class RecordingMapUpdatePort(
        private val onSend: (UUID, MapUpdate) -> Unit = { _, _ -> },
    ) : MapUpdatePort {
        override fun send(playerId: UUID, update: MapUpdate) {
            onSend(playerId, update)
        }
    }
}

private fun sampleRequest(
    sourceId: UUID,
    priority: SendPriority,
    mapId: Int,
    token: Int = mapId,
): SendRequest {
    return SendRequest(
        sourceId = sourceId,
        priority = priority,
        update = MapUpdate(
            mapId = mapId,
            mapColors = ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { token.toByte() },
        ),
        fingerprint = MapContentFingerprint(
            resourceRevision = 0,
            tileToken = token,
        ),
    )
}
