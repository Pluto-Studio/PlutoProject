package plutoproject.feature.paper.api.sit.player.events

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent
import plutoproject.feature.paper.api.sit.player.PlayerStack
import plutoproject.feature.paper.api.sit.player.PlayerStackQuitCause

class PlayerStackQuitEvent(
    player: Player,
    val stack: PlayerStack,
    val cause: PlayerStackQuitCause,
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
