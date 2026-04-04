package plutoproject.feature.gallery.core

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.display.MapUpdate
import plutoproject.feature.gallery.core.display.MapUpdatePort
import plutoproject.feature.gallery.core.display.job.SendJob
import plutoproject.feature.gallery.core.display.job.SendJobState
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SendJobTest {
    @Test
    fun `should drain queue and return to idling`() = runTest {
        val sent = mutableListOf<MapUpdate>()
        val job = newSendJob(
            scope = this,
            mapUpdatePort = RecordingMapUpdatePort(onSend = { _, update ->
                sent += update
            })
        )

        job.enqueue(sampleMapUpdate(1))
        job.enqueue(sampleMapUpdate(2))

        assertEquals(SendJobState.RUNNING, job.state)

        advanceUntilIdle()

        assertEquals(listOf(sampleMapUpdate(1), sampleMapUpdate(2)), sent)
        assertEquals(SendJobState.IDLING, job.state)
    }

    @Test
    fun `should clear old backlog when queue overflows`() = runTest {
        val sent = mutableListOf<MapUpdate>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val job = newSendJob(
            scope = this,
            loopContext = dispatcher,
            maxQueueSize = 2,
            mapUpdatePort = RecordingMapUpdatePort(onSend = { _, update ->
                sent += update
            })
        )

        val first = sampleMapUpdate(1)
        val second = sampleMapUpdate(2)
        val latest = sampleMapUpdate(3)

        job.enqueue(first)
        job.enqueue(second)
        job.enqueue(latest)

        advanceUntilIdle()

        assertEquals(listOf(latest), sent)
        assertEquals(SendJobState.IDLING, job.state)
    }

    @Test
    fun `should throttle by span size and duration`() = runTest {
        val sendTimes = mutableListOf<Long>()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val job = newSendJob(
            scope = this,
            loopContext = dispatcher,
            maxUpdatesInSpan = 2,
            updateLimitSpan = 50.milliseconds,
            mapUpdatePort = RecordingMapUpdatePort(onSend = { _, _ ->
                sendTimes += testScheduler.currentTime
            })
        )

        job.enqueue(sampleMapUpdate(1))
        job.enqueue(sampleMapUpdate(2))
        job.enqueue(sampleMapUpdate(3))

        advanceUntilIdle()

        assertEquals(listOf(0L, 0L, 50L), sendTimes)
        assertEquals(SendJobState.IDLING, job.state)
    }

    @Test
    fun `should reject enqueue after stop`() = runTest {
        val job = newSendJob(scope = this)

        job.stop()

        assertEquals(SendJobState.STOPPED, job.state)
        assertThrows(IllegalStateException::class.java) {
            job.enqueue(sampleMapUpdate(1))
        }
    }

    @Test
    fun `stop should be idempotent`() = runTest {
        val sent = mutableListOf<MapUpdate>()
        val job = newSendJob(
            scope = this,
            mapUpdatePort = RecordingMapUpdatePort(onSend = { _, update ->
                sent += update
            })
        )

        job.enqueue(sampleMapUpdate(1))
        job.stop()
        job.stop()
        advanceUntilIdle()

        assertTrue(sent.isEmpty())
        assertEquals(SendJobState.STOPPED, job.state)
    }

    private fun newSendJob(
        scope: TestScope,
        loopContext: CoroutineContext = StandardTestDispatcher(scope.testScheduler),
        maxQueueSize: Int = 8,
        maxUpdatesInSpan: Int = 2,
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

    private class RecordingMapUpdatePort(
        private val onSend: (UUID, MapUpdate) -> Unit = { _, _ -> },
    ) : MapUpdatePort {
        override fun send(playerId: UUID, update: MapUpdate) {
            onSend(playerId, update)
        }
    }
}

private fun sampleMapUpdate(mapId: Int): MapUpdate {
    return MapUpdate(
        mapId = mapId,
        mapColors = ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { mapId.toByte() },
    )
}
