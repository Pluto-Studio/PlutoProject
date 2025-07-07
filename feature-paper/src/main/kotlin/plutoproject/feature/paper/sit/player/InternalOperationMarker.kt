package plutoproject.feature.paper.sit.player

import org.bukkit.entity.Player

object InternalOperationMarker {
    private val isInOperation = mutableSetOf<Player>()

    fun isInOperation(player: Player): Boolean {
        return isInOperation.contains(player)
    }

    fun markInOperation(player: Player) {
        isInOperation.add(player)
    }

    fun unmarkInOperation(player: Player) {
        isInOperation.remove(player)
    }
}
