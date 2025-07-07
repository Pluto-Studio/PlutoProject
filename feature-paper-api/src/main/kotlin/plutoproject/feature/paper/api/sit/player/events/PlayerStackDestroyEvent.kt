package plutoproject.feature.paper.api.sit.player.events

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import plutoproject.feature.paper.api.sit.player.PlayerStack
import plutoproject.feature.paper.api.sit.player.PlayerStackDestroyCause

class PlayerStackDestroyEvent(
    val stack: PlayerStack,
    val cause: PlayerStackDestroyCause,
) : Event(), Cancellable {
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
