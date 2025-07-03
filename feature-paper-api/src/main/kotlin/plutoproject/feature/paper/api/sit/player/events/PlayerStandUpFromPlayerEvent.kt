package plutoproject.feature.paper.api.sit.player.events

import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import plutoproject.feature.paper.api.sit.SitOptions
import plutoproject.feature.paper.api.sit.block.events.StandUpEvent

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
