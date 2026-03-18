package plutoproject.feature.gallery.core

import kotlinx.coroutines.CoroutineScope
import java.time.Clock
import java.util.UUID
import kotlin.coroutines.CoroutineContext

class DefaultSendJobFactory(
    private val clock: Clock,
    private val coroutineScope: CoroutineScope,
    private val loopContext: CoroutineContext,
    private val mapUpdatePort: MapUpdatePort,
    private val maxQueueSize: Int = DEFAULT_MAX_QUEUE_SIZE,
    private val maxUpdatesInSpan: Int = DEFAULT_MAX_UPDATES_IN_SPAN,
    private val updateLimitSpanMs: Long = DEFAULT_UPDATE_LIMIT_SPAN_MS,
) : SendJobFactory {
    override fun create(playerId: UUID): SendJob {
        return DefaultSendJob(
            playerId = playerId,
            maxQueueSize = maxQueueSize,
            maxUpdatesInSpan = maxUpdatesInSpan,
            updateLimitSpanMs = updateLimitSpanMs,
            clock = clock,
            coroutineScope = coroutineScope,
            loopContext = loopContext,
            mapUpdatePort = mapUpdatePort,
        )
    }

    companion object {
        const val DEFAULT_MAX_QUEUE_SIZE: Int = 256
        const val DEFAULT_MAX_UPDATES_IN_SPAN: Int = 10
        const val DEFAULT_UPDATE_LIMIT_SPAN_MS: Long = 50
    }
}
