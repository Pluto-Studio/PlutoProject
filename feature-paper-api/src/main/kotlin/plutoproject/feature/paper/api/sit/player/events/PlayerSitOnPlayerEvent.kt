package plutoproject.feature.paper.api.sit.player.events

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent
import plutoproject.feature.paper.api.sit.SitAttemptResult
import plutoproject.feature.paper.api.sit.SitOptions

class PlayerSitOnPlayerEvent(
    val player: Player,
    val options: SitOptions,
    val attemptResult: SitAttemptResult,
    val seat: Player,
) : PlayerEvent(player), Cancellable {
    @Suppress("UNUSED")
    private companion object {
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlers
    }

    private var isCancelled = false

    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }

    override fun isCancelled(): Boolean {
        return isCancelled
    }

    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }
}
