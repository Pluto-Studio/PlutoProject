package plutoproject.feature.gallery.core.display.job

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import plutoproject.feature.gallery.core.display.MapUpdate
import plutoproject.feature.gallery.core.display.MapUpdatePort
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.coroutines.CoroutineContext

class DefaultSendJob(
    override val playerId: UUID,
    private val maxQueueSize: Int,
    private val maxUpdatesInSpan: Int,
    private val updateLimitSpanMs: Long,
    private val clock: Clock,
    private val coroutineScope: CoroutineScope,
    private val loopContext: CoroutineContext,
    private val mapUpdatePort: MapUpdatePort,
) : SendJob {
    override var state: SendJobState = SendJobState.IDLING
        private set

    private val lock = Any()
    private val queue = ArrayDeque<MapUpdate>()

    private var loopJob: Job? = null

    init {
        require(maxQueueSize > 0) { "maxQueueSize must be greater than 0" }
        require(maxUpdatesInSpan > 0) { "maxUpdatesInSpan must be greater than 0" }
        require(updateLimitSpanMs > 0) { "updateLimitSpanMs must be greater than 0" }
    }

    override fun enqueue(update: MapUpdate) {
        synchronized(lock) {
            check(state != SendJobState.STOPPED) { "SendJob is stopped" }

            if (queue.size >= maxQueueSize) {
                queue.clear()
            }

            queue.addLast(update)
            ensureLoopRunning()
        }
    }

    override fun stop() {
        synchronized(lock) {
            if (state == SendJobState.STOPPED) {
                return
            }

            val currentLoop = loopJob
            loopJob = null
            queue.clear()
            state = SendJobState.STOPPED
            currentLoop?.cancel(InternalLoopCancellation("SendJob stopped explicitly"))
        }
    }

    private fun ensureLoopRunning() {
        if (state == SendJobState.STOPPED || queue.isEmpty()) {
            return
        }

        if (loopJob?.isActive == true) {
            state = SendJobState.RUNNING
            return
        }

        loopJob = coroutineScope.launch(loopContext) {
            runLoop()
        }.also { job ->
            job.invokeOnCompletion { cause ->
                handleLoopCompletion(job, cause)
            }
        }
        state = SendJobState.RUNNING
    }

    private suspend fun runLoop() {
        currentCoroutineContext()[Job]
            ?: error("Send job loop is missing its Job")

        while (true) {
            val spanStartedAt = Instant.now(clock)
            val batch = pollNextBatch() ?: return
            if (batch.isEmpty()) {
                return
            }

            batch.forEach { update ->
                mapUpdatePort.send(playerId, update)
            }

            if (!shouldDelayForNextSpan()) {
                continue
            }

            val elapsedMillis = Duration.between(spanStartedAt, Instant.now(clock)).toMillis()
                .coerceAtLeast(0)
            val delayMillis = (updateLimitSpanMs - elapsedMillis).coerceAtLeast(0)
            if (delayMillis > 0) {
                delay(delayMillis)
            }
        }
    }

    private fun pollNextBatch(): List<MapUpdate>? = synchronized(lock) {
        if (state == SendJobState.STOPPED) {
            return@synchronized null
        }

        if (queue.isEmpty()) {
            loopJob = null
            state = SendJobState.IDLING
            return@synchronized emptyList()
        }

        buildList(capacity = minOf(maxUpdatesInSpan, queue.size)) {
            repeat(maxUpdatesInSpan) {
                val update = queue.removeFirstOrNull() ?: return@repeat
                add(update)
            }
        }
    }

    private fun shouldDelayForNextSpan(): Boolean = synchronized(lock) {
        state != SendJobState.STOPPED && queue.isNotEmpty()
    }

    private fun handleLoopCompletion(job: Job, cause: Throwable?) {
        synchronized(lock) {
            if (loopJob === job) {
                loopJob = null
            }

            if (state == SendJobState.STOPPED) {
                return
            }

            if (cause == null || cause is InternalLoopCancellation) {
                if (queue.isEmpty()) {
                    state = SendJobState.IDLING
                }
                return
            }

            queue.clear()
            state = SendJobState.STOPPED
        }
    }

    private class InternalLoopCancellation(message: String) : CancellationException(message)
}
