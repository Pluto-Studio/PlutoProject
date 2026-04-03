package plutoproject.feature.gallery.core.display.job

import kotlinx.coroutines.CoroutineScope
import plutoproject.feature.gallery.core.display.MapUpdatePort
import java.time.Clock
import java.util.UUID
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

class SendJobFactory(
    private val clock: Clock,
    private val coroutineScope: CoroutineScope,
    private val loopContext: CoroutineContext,
    private val mapUpdatePort: MapUpdatePort,
    private val maxQueueSize: Int,
    private val maxUpdatesInSpan: Int,
    private val updateLimitSpan: Duration,
) {
    fun create(playerId: UUID): SendJob {
        return SendJob(
            playerId = playerId,
            maxQueueSize = maxQueueSize,
            maxUpdatesInSpan = maxUpdatesInSpan,
            updateLimitSpan = updateLimitSpan,
            clock = clock,
            coroutineScope = coroutineScope,
            loopContext = loopContext,
            mapUpdatePort = mapUpdatePort,
        )
    }
}
