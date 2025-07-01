package plutoproject.feature.paper.api.sitV2.events

import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import plutoproject.feature.paper.api.sitV2.SitOptions

class PlayerStandUpFromPlayerEvent(
    player: Player,
    options: SitOptions,
    val sittingOn: Player
) : StandUpEvent(player, options) {
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
