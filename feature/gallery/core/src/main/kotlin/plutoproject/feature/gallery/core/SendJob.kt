package plutoproject.feature.gallery.core

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
