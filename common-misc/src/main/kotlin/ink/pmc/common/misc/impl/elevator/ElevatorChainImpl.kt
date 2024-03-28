package ink.pmc.common.misc.impl.elevator

import ink.pmc.common.misc.ELEVATOR_WORK_SOUND
import ink.pmc.common.misc.api.elevator.ElevatorChain
import ink.pmc.common.misc.elevatorGoDownTitle
import ink.pmc.common.misc.elevatorGoUpTitle
import ink.pmc.common.utils.platform.threadSafeTeleport
import ink.pmc.common.utils.world.rawLocation
import org.bukkit.Location
import org.bukkit.entity.Player

@Suppress("UNUSED")
class ElevatorChainImpl(override val floors: List<Location>, private val tpLocs: List<Location>) : ElevatorChain {

    override fun up(player: Player) {
        val next = getPreviousFloor(player)

        if (next == -1) {
            return
        }

        go(player, next)
        player.playSound(ELEVATOR_WORK_SOUND)
        player.showTitle(elevatorGoDownTitle(next, totalFloorCount()))
    }

    override fun down(player: Player) {
        val next = getNextFloor(player)

        if (next == -1) {
            return
        }

        go(player, next)
        player.playSound(ELEVATOR_WORK_SOUND)
        player.showTitle(elevatorGoUpTitle(next, totalFloorCount()))
    }

    override fun go(player: Player, floor: Int) {
        if (totalFloorCount() < floor) {
            return
        }

        val loc = tpLocs[floor]
        val tpLoc = player.location.clone()
        tpLoc.y = loc.blockY.toDouble()

        player.threadSafeTeleport(tpLoc)
    }

    override fun getNextFloor(player: Player): Int {
        if (getCurrentFloor(player) == -1 || getCurrentFloor(player) == floors.size) {
            return -1
        }

        return getCurrentFloor(player) + 1
    }

    override fun getPreviousFloor(player: Player): Int {
        if (getCurrentFloor(player) == -1 || getCurrentFloor(player) == 1) {
            return -1
        }

        return getCurrentFloor(player) - 1
    }

    override fun getCurrentFloor(player: Player): Int {
        val floorLoc = player.location.rawLocation.subtract(0.0, 1.0, 0.0)

        if (!floors.contains(floorLoc)) {
            return -1
        }

        return floors.indexOf(floorLoc) + 1
    }

    override fun totalFloorCount(): Int {
        return floors.size
    }

}