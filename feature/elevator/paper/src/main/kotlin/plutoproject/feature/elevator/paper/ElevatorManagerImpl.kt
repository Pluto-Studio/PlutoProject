package plutoproject.feature.elevator.paper

import org.bukkit.Location
import org.bukkit.Material
import plutoproject.feature.elevator.api.paper.ElevatorBuilder
import plutoproject.feature.elevator.api.paper.ElevatorChain
import plutoproject.feature.elevator.api.paper.ElevatorManager
import plutoproject.foundation.paper.world.viewAligned

class ElevatorManagerImpl : ElevatorManager {
    private val materialToBuilderMap = mutableMapOf<Material, ElevatorBuilder>()

    override val builders: Map<Material, ElevatorBuilder>
        get() = materialToBuilderMap

    override fun registerBuilder(builder: ElevatorBuilder) {
        materialToBuilderMap[builder.type] = builder
    }

    override suspend fun getChainAt(loc: Location): ElevatorChain? {
        val offsetLoc = loc.clone().subtract(0.0, 1.0, 0.0).viewAligned()
        val type = offsetLoc.block.type
        if (!materialToBuilderMap.containsKey(type)) {
            return null
        }
        val builder = materialToBuilderMap[type]!!
        if (builder.teleportLocations(loc).size < 2) {
            return null
        }
        val chain = ElevatorChainImpl(builder.findLocations(loc), builder.teleportLocations(loc))
        return chain
    }
}
