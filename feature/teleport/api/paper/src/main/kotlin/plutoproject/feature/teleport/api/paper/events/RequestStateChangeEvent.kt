package plutoproject.feature.teleport.api.paper.events

import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent
import plutoproject.feature.teleport.api.paper.RequestState
import plutoproject.feature.teleport.api.paper.TeleportRequest

@Suppress("UNUSED")
class RequestStateChangeEvent(
    val request: TeleportRequest,
    val before: RequestState,
    val after: RequestState
) : PlayerEvent(request.source, true), Cancellable {
    private companion object {
        val handlers = HandlerList()

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

    override fun setCancelled(isCancelled: Boolean) {
        this.isCancelled = isCancelled
    }
}
