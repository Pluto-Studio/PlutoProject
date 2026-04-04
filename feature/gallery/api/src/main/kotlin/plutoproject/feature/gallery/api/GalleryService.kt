package plutoproject.feature.gallery.api

import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KClass

interface GalleryService {
    fun <T : GalleryEvent> subscribeEvent(eventType: KClass<T>): Flow<T>
}

inline fun <reified T : GalleryEvent> GalleryService.subscribeEvent(): Flow<T> {
    return subscribeEvent(T::class)
}
