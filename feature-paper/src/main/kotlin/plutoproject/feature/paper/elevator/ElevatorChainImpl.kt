package plutoproject.feature.paper.elevator

import org.bukkit.Location
import org.bukkit.entity.Player
import plutoproject.feature.paper.api.elevator.ElevatorChain
import plutoproject.framework.common.util.chat.title.subTitleReplace
import plutoproject.framework.paper.util.world.location.viewAligned

class ElevatorChainImpl(override val floors: List<Location>, private val tpLocs: List<Location>) : ElevatorChain {
    override fun up(player: Player) {
        val next = getNextFloor(player)
        if (next == -1) return
        go(player, next)
        player.playSound(elevatorWorking)
        val title = elevatorGoUp
            .subTitleReplace("<curr>", next)
            .subTitleReplace("<total>", totalFloorCount())
        player.showTitle(title)
    }

    override fun down(player: Player) {
        val next = getPreviousFloor(player)
        if (next == -1) return
        go(player, next)
        player.playSound(elevatorWorking)
        val title = elevatorGoDown
            .subTitleReplace("<curr>", next)
            .subTitleReplace("<total>", totalFloorCount())
        player.showTitle(title)
    }

    override fun go(player: Player, floor: Int) {
        if (totalFloorCount() < floor) return
        val loc = tpLocs[floor - 1]
        val tpLoc = player.location.clone()
        tpLoc.y = loc.blockY.toDouble()
        player.teleport(tpLoc)
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
        val floorLoc = player.location.viewAligned().subtract(0.0, 1.0, 0.0)
        if (!floors.contains(floorLoc)) {
            return -1
        }
        return floors.indexOf(floorLoc) + 1
    }

    override fun totalFloorCount(): Int {
        return floors.size
    }
}
