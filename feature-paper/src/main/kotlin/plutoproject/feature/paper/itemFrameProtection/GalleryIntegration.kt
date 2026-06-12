package plutoproject.feature.paper.itemFrameProtection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.entity.ItemFrame
import plutoproject.feature.gallery.api.GalleryEvent
import plutoproject.feature.gallery.api.GalleryService
import plutoproject.feature.gallery.api.subscribeEvent
import plutoproject.feature.gallery.adapter.paper.imageItemFrameData
import plutoproject.feature.gallery.core.display.DisplayInstanceStore
import plutoproject.framework.common.api.feature.FeatureManager
import plutoproject.framework.paper.util.coroutine.coroutineContext
import plutoproject.framework.paper.util.server

object GalleryIntegration {
    val isAvailable: Boolean
        get() = FeatureManager.isEnabled("gallery")

    val service: GalleryService?
        get() = getFromGalleryFeature()

    val displayInstanceStore: DisplayInstanceStore?
        get() = getFromGalleryFeature()

    fun isGalleryItemFrame(itemFrame: ItemFrame): Boolean {
        return isAvailable && itemFrame.imageItemFrameData() != null
    }

    fun start(coroutineScope: CoroutineScope) {
        val galleryService = service ?: return

        galleryService.subscribeEvent<GalleryEvent.ImagePlacement>()
            .onEach(::handlePlacement)
            .launchIn(coroutineScope)

        galleryService.subscribeEvent<GalleryEvent.ImageRemoval>()
            .onEach(::handleRemoval)
            .launchIn(coroutineScope)
    }

    private suspend fun handlePlacement(event: GalleryEvent.ImagePlacement) {
        if (event.imageDisplay == null) return
        val player = Bukkit.getPlayer(event.player) ?: return

        withContext(server.coroutineContext) {
            event.itemFrames.mapNotNull { Bukkit.getEntity(it) as? ItemFrame }
                .forEach { itemFrame ->
                    itemFrame.inv = true
                    itemFrame.setProtect(true, player)
                }
            player.sendMessage(GALLERY_ITEMFRAME_AUTO_PROTECTED)
        }
    }

    private suspend fun handleRemoval(event: GalleryEvent.ImageRemoval) {
        withContext(server.coroutineContext) {
            event.itemFrames.mapNotNull { Bukkit.getEntity(it) as? ItemFrame }
                .forEach { itemFrame ->
                    itemFrame.inv = false
                    itemFrame.clearProtect()
                }
        }
    }

    private inline fun <reified T : Any> getFromGalleryFeature(): T? {
        if (!isAvailable) return null
        return runCatching {
            FeatureManager.getFeature("gallery")?.featureKoin?.get<T>()
        }.getOrNull()
    }
}
