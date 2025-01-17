package plutoproject.feature.paper.api.home

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import plutoproject.feature.paper.api.teleport.AbstractTeleportEvent

class HomeTeleportEvent(
    player: Player,
    from: Location,
    val home: Home,
) : AbstractTeleportEvent(player, from, home.location) {
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
