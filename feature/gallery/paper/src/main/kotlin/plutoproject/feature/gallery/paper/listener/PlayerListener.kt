package plutoproject.feature.gallery.paper.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import plutoproject.feature.gallery.common.upload.UploadService
import plutoproject.feature.gallery.paper.unlockImageItemCopyRecipeFor
import plutoproject.feature.gallery.core.display.DisplayRuntimeRegistry
import plutoproject.feature.gallery.core.display.job.SendJobRegistry
import plutoproject.kernel.api.koinGet

@Suppress("UNUSED")
object PlayerListener : Listener {
    private val sendJobRegistry = koinGet<SendJobRegistry>()
    private val displayRuntime = koinGet<DisplayRuntimeRegistry>()
    private val uploadService = koinGet<UploadService>()

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        sendJobRegistry.start(event.player.uniqueId)
        unlockImageItemCopyRecipeFor(event.player)
    }

    @EventHandler
    suspend fun onPlayerQuit(event: PlayerQuitEvent) {
        val uniqueId = event.player.uniqueId
        uploadService.cancelUnfinishedSession(uniqueId)
        displayRuntime.clearPlayerCache(uniqueId)
        sendJobRegistry.stop(uniqueId)
    }
}
