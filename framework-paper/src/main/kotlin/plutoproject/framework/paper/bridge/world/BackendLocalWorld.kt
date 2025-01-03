package plutoproject.framework.paper.bridge.world

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import plutoproject.framework.common.api.bridge.Bridge
import plutoproject.framework.common.api.bridge.server.BridgeGroup
import plutoproject.framework.common.api.bridge.server.BridgeServer
import plutoproject.framework.common.api.bridge.server.ServerState
import plutoproject.framework.common.api.bridge.server.ServerType
import plutoproject.framework.common.api.bridge.world.BridgeLocation
import plutoproject.framework.common.api.bridge.world.BridgeWorld
import plutoproject.framework.common.bridge.throwLocalWorldNotFound
import plutoproject.framework.common.bridge.world.BridgeLocationImpl
import plutoproject.framework.common.bridge.world.InternalWorld
import plutoproject.framework.paper.api.worldalias.alias
import plutoproject.framework.paper.bridge.server.localServer

internal fun BridgeWorld.getBukkit(): World {
    check(serverState.isLocal) { "Only local location can be converted" }
    return Bukkit.getWorld(name)!!
}

internal fun World.getBridge(): BridgeWorld? {
    return localServer.getWorld(name)
}

internal fun Location.createBridge(): BridgeLocation {
    val localWorld = world.getBridge() ?: throwLocalWorldNotFound(world.name)
    return BridgeLocationImpl(Bridge.local, localWorld, x, y, z, yaw, pitch)
}

internal fun BridgeLocation.createBukkit(): Location {
    return Location(world.getBukkit(), x, y, z, yaw, pitch)
}

class BackendLocalWorld(private val actual: World, override val server: BridgeServer) : InternalWorld() {
    override val group: BridgeGroup? = server.group
    override val serverType: ServerType = ServerType.BACKEND
    override val serverState: ServerState = ServerState.LOCAL
    override val name: String = actual.name
    override val alias: String? = actual.alias
    override var spawnPoint: BridgeLocation
        get() = actual.spawnLocation.createBridge()
        set(_) = error("Unsupported")

    override fun getLocation(x: Double, y: Double, z: Double, yaw: Float, pitch: Float): BridgeLocation {
        return BridgeLocationImpl(server, this, x, y, z, yaw, pitch)
    }
}
