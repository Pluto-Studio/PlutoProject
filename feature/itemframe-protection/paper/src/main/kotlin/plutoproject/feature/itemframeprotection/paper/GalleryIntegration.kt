package plutoproject.feature.itemframeprotection.paper

import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import org.bukkit.Bukkit
import org.bukkit.entity.ItemFrame
import plutoproject.feature.gallery.api.GalleryEvent
import plutoproject.feature.gallery.api.GalleryService
import plutoproject.feature.gallery.api.subscribeEvent
import plutoproject.feature.gallery.paper.imageItemFrameData
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.getServiceOrNull
import plutoproject.kernel.api.paper.PaperModuleContext
import java.util.UUID

object GalleryIntegration {
    private val galleryService: GalleryService?
        get() = currentModuleContext().services.getServiceOrNull()

    private val serverContext
        get() = (currentModuleContext() as PaperModuleContext).plugin.minecraftDispatcher

    val isAvailable: Boolean
        get() = galleryService != null

    fun isGalleryItemFrame(itemFrame: ItemFrame): Boolean {
        return isAvailable && itemFrame.imageItemFrameData() != null
    }

    suspend fun getDisplayItemFrameIds(displayInstanceId: UUID): List<UUID>? {
        return galleryService?.getDisplayItemFrameIds(displayInstanceId)
    }

    fun start() {
        val galleryService = galleryService ?: return
        val coroutineScope = currentModuleContext().coroutineScope

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

        withContext(serverContext) {
            event.itemFrames.mapNotNull { Bukkit.getEntity(it) as? ItemFrame }
                .forEach { itemFrame ->
                    itemFrame.inv = true
                    itemFrame.setProtect(true, player)
                }
            player.sendMessage(GALLERY_ITEMFRAME_AUTO_PROTECTED)
        }
    }

    private suspend fun handleRemoval(event: GalleryEvent.ImageRemoval) {
        withContext(serverContext) {
            event.itemFrames.mapNotNull { Bukkit.getEntity(it) as? ItemFrame }
                .forEach { itemFrame ->
                    itemFrame.inv = false
                    itemFrame.clearProtect()
                }
        }
    }

}
