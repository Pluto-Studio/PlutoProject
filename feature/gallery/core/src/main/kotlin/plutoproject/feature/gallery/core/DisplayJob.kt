package plutoproject.feature.gallery.core

import java.util.*

interface DisplayJob {
    val managedDisplayInstances: Map<UUID, DisplayInstance>

    fun wake()

    fun cleanup()
}
