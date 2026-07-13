package plutoproject.feature.gallery.api

import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlin.reflect.KClass

interface GalleryService {
    fun <T : GalleryEvent> subscribeEvent(eventType: KClass<T>): Flow<T>

    suspend fun getDisplayItemFrameIds(displayInstanceId: UUID): List<UUID>?
}

inline fun <reified T : GalleryEvent> GalleryService.subscribeEvent(): Flow<T> {
    return subscribeEvent(T::class)
}
