package plutoproject.feature.gallery.adapter.common

import kotlinx.coroutines.flow.Flow
import plutoproject.feature.gallery.api.GalleryEvent
import plutoproject.feature.gallery.api.GalleryService
import kotlin.reflect.KClass

abstract class CommonGalleryService : GalleryService {
    override fun <T : GalleryEvent> subscribeEvent(eventType: KClass<T>): Flow<T> {
        TODO("Not yet implemented")
    }
}
