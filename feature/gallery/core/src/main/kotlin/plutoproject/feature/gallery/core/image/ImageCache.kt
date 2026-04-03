package plutoproject.feature.gallery.core.image

import kotlinx.coroutines.CoroutineScope
import plutoproject.feature.gallery.core.util.ResourceCache
import java.time.Clock
import java.util.*
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

class ImageCache(
    coroutineScope: CoroutineScope,
    coroutineContext: CoroutineContext,
    clock: Clock,
    logger: Logger
) : ResourceCache<UUID, Image, Unit>(coroutineScope, coroutineContext, clock, logger) {
    override fun keyOf(value: Image): UUID {
        return value.id
    }

    override fun buildIndex(value: Image) = Unit
}
