package plutoproject.feature.gallery.core

import java.util.*

interface DisplayJob {
    val isStopped: Boolean
    val managedDisplayInstances: Map<UUID, DisplayInstance>

    fun wake()

    fun stop()
}
