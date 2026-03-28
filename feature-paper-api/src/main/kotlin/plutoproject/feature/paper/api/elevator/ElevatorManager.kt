package plutoproject.feature.paper.api.elevator

import org.bukkit.Location
import org.bukkit.Material
import plutoproject.framework.common.util.inject.globalKoin

interface ElevatorManager {
    companion object : ElevatorManager by globalKoin.get()

    val builders: Map<Material, ElevatorBuilder>

    fun registerBuilder(builder: ElevatorBuilder)

    suspend fun getChainAt(loc: Location): ElevatorChain?
}
