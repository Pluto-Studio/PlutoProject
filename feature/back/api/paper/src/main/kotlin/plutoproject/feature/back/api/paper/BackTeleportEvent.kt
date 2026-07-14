package plutoproject.feature.back.api.paper

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import plutoproject.feature.teleport.api.paper.AbstractTeleportEvent

class BackTeleportEvent(
    player: Player,
    from: Location,
    to: Location,
) : AbstractTeleportEvent(player, from, to) {
    @Suppress("UNUSED")
    private companion object {
        val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlers
    }

    private var cancelled = false

    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled(bool: Boolean) {
        cancelled = bool
    }
}
