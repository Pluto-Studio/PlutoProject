package ink.pmc.misc.impl.elevator

import ink.pmc.misc.api.elevator.ElevatorBuilder
import ink.pmc.misc.api.elevator.ElevatorChain
import ink.pmc.misc.api.elevator.ElevatorManager
import ink.pmc.framework.world.eraseAngle
import org.bukkit.Location
import org.bukkit.Material

class ElevatorManagerImpl : ElevatorManager {

    private val materialToBuilderMap = mutableMapOf<Material, ElevatorBuilder>()

    override val builders: Map<Material, ElevatorBuilder>
        get() = materialToBuilderMap

    override fun registerBuilder(builder: ElevatorBuilder) {
        materialToBuilderMap[builder.type] = builder
    }

    override suspend fun getChainAt(loc: Location): ElevatorChain? {
        val offsetLoc = loc.clone().subtract(0.0, 1.0, 0.0).eraseAngle()

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