package ink.pmc.framework.bridge.world

import ink.pmc.framework.bridge.Bridge
import ink.pmc.framework.bridge.proto.BridgeRpcOuterClass.LocationInfo
import ink.pmc.framework.bridge.proto.locationInfo
import ink.pmc.framework.bridge.server.BridgeServer

fun LocationInfo.createBridge(server: BridgeServer? = null, world: BridgeWorld? = null): BridgeLocation {
    val actualServer = server
        ?: Bridge.getServer(this.server)
        ?: error("Server not found: ${this.server}")
    val actualWorld = world
        ?: actualServer.getWorld(this.world)
        ?: error("World not found: ${this.world} (server: ${actualServer.id})")
    return BridgeLocationImpl(actualServer, actualWorld, x, y, z, yaw, pitch)
}

fun BridgeLocation.createInfo(): LocationInfo {
    val loc = this
    return locationInfo {
        server = loc.server.id
        world = loc.world.name
        x = loc.x
        y = loc.y
        z = loc.z
        yaw = loc.yaw
        pitch = loc.pitch
    }
}

data class BridgeLocationImpl(
    override val server: BridgeServer,
    override val world: BridgeWorld,
    override val x: Double,
    override val y: Double,
    override val z: Double,
    override val yaw: Float = 0.0F,
    override val pitch: Float = 0.0F
) : BridgeLocation