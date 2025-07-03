package plutoproject.feature.paper.api.sit.player.events

import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import plutoproject.feature.paper.api.sit.SitAttemptResult
import plutoproject.feature.paper.api.sit.SitOptions
import plutoproject.feature.paper.api.sit.block.events.SitEvent

class PlayerSitOnPlayerEvent(
    player: Player,
    options: SitOptions,
    attemptResult: SitAttemptResult,
    val sittingOn: Player,
) : SitEvent(player, options, attemptResult) {
    @Suppress("UNUSED")
    private companion object {
        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlers
    }

    private var isCancelled = false

    override fun getHandlers() = Companion.handlers

    override fun isCancelled() = isCancelled

    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }
}
