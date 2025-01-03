package ink.pmc.essentials.api.back

import ink.pmc.essentials.api.teleport.AbstractTeleportEvent
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

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