package plutoproject.feature.gallery.adapter.common

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import plutoproject.feature.gallery.api.GalleryEvent
import plutoproject.feature.gallery.api.GalleryService
import kotlin.reflect.KClass

abstract class CommonGalleryService : GalleryService {
    private val events = MutableSharedFlow<GalleryEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_LATEST,
    )

    override fun <T : GalleryEvent> subscribeEvent(eventType: KClass<T>): Flow<T> {
        return events.filterIsInstance(eventType)
    }

    suspend fun publish(event: GalleryEvent) {
        events.emit(event)
    }
}
