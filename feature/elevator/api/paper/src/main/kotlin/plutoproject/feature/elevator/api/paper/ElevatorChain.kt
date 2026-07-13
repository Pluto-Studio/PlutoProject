package plutoproject.feature.elevator.api.paper

import org.bukkit.Location
import org.bukkit.entity.Player

interface ElevatorChain {
    val floors: List<Location>

    fun up(player: Player)

    fun down(player: Player)

    fun go(player: Player, floor: Int)

    fun getNextFloor(player: Player): Int

    fun getPreviousFloor(player: Player): Int

    fun getCurrentFloor(player: Player): Int

    fun totalFloorCount(): Int
}
