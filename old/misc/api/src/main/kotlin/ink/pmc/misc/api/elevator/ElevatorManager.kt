package ink.pmc.misc.api.elevator

import ink.pmc.framework.inject.inlinedGet
import org.bukkit.Location
import org.bukkit.Material

@Suppress("UNUSED")
interface ElevatorManager {

    companion object : ElevatorManager by inlinedGet()

    val builders: Map<Material, ElevatorBuilder>

    fun registerBuilder(builder: ElevatorBuilder)

    suspend fun getChainAt(loc: Location): ElevatorChain?

}