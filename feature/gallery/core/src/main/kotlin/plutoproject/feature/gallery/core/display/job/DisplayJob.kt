package plutoproject.feature.gallery.core.display.job

import plutoproject.feature.gallery.core.display.DisplayInstance
import java.util.*

interface DisplayJob {
    val belongsTo: UUID
    val isStopped: Boolean
    val attachedDisplayInstances: Map<UUID, DisplayInstance>

    fun attach(displayInstance: DisplayInstance)

    fun detach(displayInstanceId: UUID): DisplayInstance?

    fun isEmpty(): Boolean

    fun wake()

    fun stop()
}
