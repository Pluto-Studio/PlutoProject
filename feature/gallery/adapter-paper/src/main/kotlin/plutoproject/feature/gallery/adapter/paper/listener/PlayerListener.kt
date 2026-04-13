package plutoproject.feature.gallery.adapter.paper.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import plutoproject.feature.gallery.adapter.common.koin
import plutoproject.feature.gallery.adapter.common.upload.UploadService
import plutoproject.feature.gallery.core.display.DisplayRuntimeRegistry
import plutoproject.feature.gallery.core.display.job.SendJobRegistry

@Suppress("UNUSED")
object PlayerListener : Listener {
    private val sendJobRegistry = koin.get<SendJobRegistry>()
    private val displayRuntime = koin.get<DisplayRuntimeRegistry>()
    private val uploadService = koin.get<UploadService>()

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        sendJobRegistry.start(event.player.uniqueId)
    }

    @EventHandler
    suspend fun onPlayerQuit(event: PlayerQuitEvent) {
        val uniqueId = event.player.uniqueId
        uploadService.cancelUnfinishedSession(uniqueId)
        displayRuntime.clearPlayerCache(uniqueId)
        sendJobRegistry.stop(uniqueId)
    }
}
