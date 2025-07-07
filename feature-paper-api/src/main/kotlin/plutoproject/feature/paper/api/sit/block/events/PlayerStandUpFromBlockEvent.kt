package plutoproject.feature.paper.api.sit.block.events

import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent
import plutoproject.feature.paper.api.sit.SitOptions
import plutoproject.feature.paper.api.sit.block.StandUpFromBlockCause

class PlayerStandUpFromBlockEvent(
    player: Player,
    val options: SitOptions,
    val cause: StandUpFromBlockCause,
    val seat: Block,
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
