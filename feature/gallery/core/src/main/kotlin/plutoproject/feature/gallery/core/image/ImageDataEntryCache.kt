package plutoproject.feature.gallery.core.image

import kotlinx.coroutines.CoroutineScope
import plutoproject.feature.gallery.core.util.ResourceCache
import java.time.Clock
import java.util.*
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

class ImageDataEntryCache(
    coroutineScope: CoroutineScope,
    coroutineContext: CoroutineContext,
    clock: Clock,
    logger: Logger
) : ResourceCache<UUID, ImageDataEntry<*>, Unit>(coroutineScope, coroutineContext, clock, logger) {
    override fun keyOf(value: ImageDataEntry<*>): UUID {
        return value.imageId
    }

    override fun buildIndex(value: ImageDataEntry<*>) = Unit
}
