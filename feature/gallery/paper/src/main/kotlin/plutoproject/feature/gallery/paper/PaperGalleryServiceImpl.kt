package plutoproject.feature.gallery.paper

import plutoproject.feature.gallery.common.GalleryServiceImpl
import plutoproject.feature.gallery.core.display.DisplayInstanceStore
import plutoproject.kernel.api.koinInject
import java.util.UUID

class PaperGalleryServiceImpl : GalleryServiceImpl() {
    private val displayInstanceStore by koinInject<DisplayInstanceStore>()

    override suspend fun getDisplayItemFrameIds(displayInstanceId: UUID): List<UUID>? {
        return displayInstanceStore.get(displayInstanceId)?.itemFrameIds
    }
}
