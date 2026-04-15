package plutoproject.feature.gallery.core.display.job

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.time.onTimeout
import plutoproject.feature.gallery.core.display.MapUpdate
import plutoproject.feature.gallery.core.display.MapUpdatePort
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlin.collections.ArrayDeque
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import java.time.Duration as JavaDuration

enum class SendPriority {
    STATIC,
    ANIMATED,
}

data class MapContentFingerprint(
    val resourceRevision: Long,
    val tileToken: Int,
)

data class SendRequest(
    val sourceId: UUID,
    val priority: SendPriority,
    val update: MapUpdate,
    val fingerprint: MapContentFingerprint,
)

class SendJob(
    val playerId: UUID,
    private val maxQueueSize: Int,
    private val maxUpdatesInSpan: Int,
    private val updateLimitSpan: Duration,
    private val clock: Clock,
    coroutineScope: CoroutineScope,
    loopContext: CoroutineContext,
    private val mapUpdatePort: MapUpdatePort,
) {
    private val channel = Channel<Event>(Channel.UNLIMITED)
    private val actorJob: Job = coroutineScope.launch(loopContext) {
        runActor()
    }.also { job ->
        job.invokeOnCompletion {
            channel.close()
        }
    }

    init {
        require(maxQueueSize > 0) { "maxQueueSize must be greater than 0" }
        require(maxUpdatesInSpan > 0) { "maxUpdatesInSpan must be greater than 0" }
        require(updateLimitSpan > Duration.ZERO) { "updateLimitSpan must be greater than 0" }
    }

    fun enqueue(request: SendRequest) {
        sendExternalEvent(Event.Enqueue(request))
    }

    fun pause() {
        sendExternalEvent(Event.Pause)
    }

    fun resume() {
        sendExternalEvent(Event.Resume)
    }

    fun stop() {
        if (!actorJob.isActive) {
            return
        }
        channel.trySend(Event.Stop)
    }

    private fun sendExternalEvent(event: Event) {
        check(actorJob.isActive && channel.trySend(event).isSuccess) { "SendJob is stopped" }
    }

    private suspend fun runActor() {
        val context = Context()
        var state: State = State.Idle

        while (state != State.Stopped) {
            val event = waitEvent(state, clock.instant())
            state = reduce(state, event, clock.instant(), context)

            while (state != State.Stopped) {
                val nextEvent = channel.tryReceive().getOrNull() ?: break
                state = reduce(state, nextEvent, clock.instant(), context)
            }
        }
    }

    private suspend fun waitEvent(state: State, now: Instant): Event {
        return when (state) {
            State.Idle, State.Paused -> channel.receive()
            is State.Active -> {
                channel.tryReceive().getOrNull()?.let { return it }

                val nextSendAt = state.nextSendAt
                val canSendNow = nextSendAt == null || !now.isBefore(nextSendAt)
                if (canSendNow) {
                    Event.Tick
                } else {
                    val waitDuration = JavaDuration.between(now, nextSendAt)
                    select<Event> {
                        channel.onReceive { it }
                        onTimeout(waitDuration) { Event.Tick }
                    }
                }
            }

            State.Stopped -> error("Should not wait in stopped state")
        }
    }

    private fun reduce(
        state: State,
        event: Event,
        now: Instant,
        context: Context,
    ): State {
        return when (state) {
            State.Idle -> reduceIdle(event, context)
            is State.Active -> reduceActive(state, event, now, context)
            State.Paused -> reducePaused(event, context)
            State.Stopped -> State.Stopped
        }
    }

    private fun reduceIdle(event: Event, context: Context): State {
        return when (event) {
            is Event.Enqueue -> {
                enqueuePending(context, event.request)
                nextStateFromPending(context, nextSendAt = null)
            }

            Event.Pause -> State.Paused
            Event.Resume, Event.Tick -> State.Idle
            Event.Stop -> stopContext(context)
        }
    }

    private fun reduceActive(state: State.Active, event: Event, now: Instant, context: Context): State {
        return when (event) {
            is Event.Enqueue -> {
                enqueuePending(context, event.request)
                nextStateFromPending(context, nextSendAt = state.nextSendAt)
            }

            Event.Tick -> {
                val sentCount = sendBatch(context)
                if (sentCount == 0) {
                    State.Idle
                } else {
                    nextStateFromPending(context, nextSendAt = now.plus(updateLimitSpan.toJavaDuration()))
                }
            }

            Event.Pause -> State.Paused
            Event.Resume -> nextStateFromPending(context, nextSendAt = state.nextSendAt)
            Event.Stop -> stopContext(context)
        }
    }

    private fun reducePaused(event: Event, context: Context): State {
        return when (event) {
            is Event.Enqueue -> {
                enqueuePending(context, event.request)
                State.Paused
            }

            Event.Resume -> nextStateFromPending(context, nextSendAt = null)
            Event.Pause, Event.Tick -> State.Paused
            Event.Stop -> stopContext(context)
        }
    }

    private fun stopContext(context: Context): State {
        context.clear()
        return State.Stopped
    }

    private fun nextStateFromPending(context: Context, nextSendAt: Instant?): State {
        if (!context.hasPending()) {
            return State.Idle
        }

        return State.Active(nextSendAt)
    }

    private fun enqueuePending(context: Context, request: SendRequest) {
        val mapId = request.update.mapId
        if (context.lastSentFingerprintByMapId[mapId] == request.fingerprint) {
            return
        }

        val (pendingBySource, sourceOrder) = context.pendingBucket(request.priority)
        val sourceQueue = pendingBySource.getOrPut(request.sourceId) { LinkedHashMap() }
        val existing = sourceQueue[mapId]

        if (existing != null) {
            if (existing.fingerprint == request.fingerprint) {
                return
            }

            sourceQueue[mapId] = PendingUpdate(request)
            return
        }

        if (context.pendingCount >= maxQueueSize) {
            when (request.priority) {
                SendPriority.ANIMATED -> return
                SendPriority.STATIC -> evictOneAnimated(context)
            }
        }

        if (sourceQueue.isEmpty()) {
            sourceOrder.addLast(request.sourceId)
        }

        sourceQueue[mapId] = PendingUpdate(request)
        context.pendingCount++
    }

    private fun evictOneAnimated(context: Context) {
        while (context.animatedSourceOrder.isNotEmpty()) {
            val sourceId = context.animatedSourceOrder.removeFirst()
            val sourceQueue = context.animatedPendingBySource[sourceId] ?: continue
            val iterator = sourceQueue.entries.iterator()
            if (!iterator.hasNext()) {
                context.animatedPendingBySource.remove(sourceId)
                continue
            }

            iterator.next()
            iterator.remove()
            context.pendingCount--

            if (sourceQueue.isEmpty()) {
                context.animatedPendingBySource.remove(sourceId)
            } else {
                context.animatedSourceOrder.addLast(sourceId)
            }
            return
        }
    }

    private fun sendBatch(context: Context): Int {
        var sentCount = 0

        repeat(maxUpdatesInSpan) {
            val pending = pollNextPending(context) ?: return@repeat
            mapUpdatePort.send(playerId, pending.update)
            context.lastSentFingerprintByMapId[pending.update.mapId] = pending.fingerprint
            sentCount++
        }

        return sentCount
    }

    private fun pollNextPending(context: Context): PendingUpdate? {
        return pollNextPending(
            pendingBySource = context.staticPendingBySource,
            sourceOrder = context.staticSourceOrder,
            context = context,
        ) ?: pollNextPending(
            pendingBySource = context.animatedPendingBySource,
            sourceOrder = context.animatedSourceOrder,
            context = context,
        )
    }

    private fun pollNextPending(
        pendingBySource: MutableMap<UUID, LinkedHashMap<Int, PendingUpdate>>,
        sourceOrder: ArrayDeque<UUID>,
        context: Context,
    ): PendingUpdate? {
        while (sourceOrder.isNotEmpty()) {
            val sourceId = sourceOrder.removeFirst()
            val sourceQueue = pendingBySource[sourceId] ?: continue
            val iterator = sourceQueue.entries.iterator()

            if (!iterator.hasNext()) {
                pendingBySource.remove(sourceId)
                continue
            }

            val pending = iterator.next().value
            iterator.remove()
            context.pendingCount--

            if (sourceQueue.isEmpty()) {
                pendingBySource.remove(sourceId)
            } else {
                sourceOrder.addLast(sourceId)
            }

            return pending
        }

        return null
    }

    private sealed interface State {
        data object Idle : State

        data class Active(
            val nextSendAt: Instant?,
        ) : State

        data object Paused : State

        data object Stopped : State
    }

    private sealed interface Event {
        data class Enqueue(val request: SendRequest) : Event
        data object Tick : Event
        data object Pause : Event
        data object Resume : Event
        data object Stop : Event
    }

    private data class PendingUpdate(
        val update: MapUpdate,
        val fingerprint: MapContentFingerprint,
    ) {
        constructor(request: SendRequest) : this(
            update = request.update,
            fingerprint = request.fingerprint,
        )
    }

    private class Context {
        val lastSentFingerprintByMapId = HashMap<Int, MapContentFingerprint>()
        val staticPendingBySource = HashMap<UUID, LinkedHashMap<Int, PendingUpdate>>()
        val animatedPendingBySource = HashMap<UUID, LinkedHashMap<Int, PendingUpdate>>()
        val staticSourceOrder = ArrayDeque<UUID>()
        val animatedSourceOrder = ArrayDeque<UUID>()

        var pendingCount: Int = 0

        fun hasPending(): Boolean {
            return pendingCount > 0
        }

        fun pendingBucket(priority: SendPriority): Pair<MutableMap<UUID, LinkedHashMap<Int, PendingUpdate>>, ArrayDeque<UUID>> {
            return when (priority) {
                SendPriority.STATIC -> staticPendingBySource to staticSourceOrder
                SendPriority.ANIMATED -> animatedPendingBySource to animatedSourceOrder
            }
        }

        fun clear() {
            lastSentFingerprintByMapId.clear()
            staticPendingBySource.clear()
            animatedPendingBySource.clear()
            staticSourceOrder.clear()
            animatedSourceOrder.clear()
            pendingCount = 0
        }
    }
}
