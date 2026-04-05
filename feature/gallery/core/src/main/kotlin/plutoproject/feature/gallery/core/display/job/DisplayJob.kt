package plutoproject.feature.gallery.core.display.job

import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.image.ImageType
import java.util.*

interface DisplayJob {
    val imageId: UUID
    val type: ImageType
    val isStopped: Boolean
    val attachedDisplayInstances: Map<UUID, DisplayInstance>

    fun attach(displayInstance: DisplayInstance)

    fun detach(displayInstanceId: UUID): DisplayInstance?

    fun replaceResource(resource: DisplayResource)

    fun isEmpty(): Boolean

    fun wake()

    fun stop()
}
