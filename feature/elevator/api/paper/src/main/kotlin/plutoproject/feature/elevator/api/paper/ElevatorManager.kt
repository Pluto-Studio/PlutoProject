package plutoproject.feature.elevator.api.paper

import org.bukkit.Location
import org.bukkit.Material

interface ElevatorManager {
    val builders: Map<Material, ElevatorBuilder>

    fun registerBuilder(builder: ElevatorBuilder)

    suspend fun getChainAt(loc: Location): ElevatorChain?
}
