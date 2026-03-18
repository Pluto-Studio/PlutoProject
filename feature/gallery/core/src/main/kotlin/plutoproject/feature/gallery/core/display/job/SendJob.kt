package plutoproject.feature.gallery.core.display.job

import plutoproject.feature.gallery.core.display.MapUpdate
import java.util.UUID

interface SendJob {
    val playerId: UUID

    val state: SendJobState

    fun enqueue(update: MapUpdate)

    fun stop()
}

enum class SendJobState {
    RUNNING,
    IDLING,
    STOPPED,
}
