package ink.pmc.essentials.api.teleport

import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

@Suppress("UNUSED")
class TeleportEvent(
    player: Player,
    from: Location,
    to: Location,
    val options: TeleportOptions
) : AbstractTeleportEvent(player, from, to) {

    private companion object {
        val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList() = handlers
    }

    private var _isDenied = false
    private var _deniedReason: Component? = null
    private var cancelled = false

    val isDenied: Boolean
        get() = _isDenied
    val deniedReason: Component?
        get() = _deniedReason

    fun denied(reason: Component?) {
        _isDenied = true
        _deniedReason = reason
    }

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