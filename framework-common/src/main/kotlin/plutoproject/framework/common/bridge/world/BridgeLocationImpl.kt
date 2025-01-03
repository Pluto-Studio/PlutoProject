package plutoproject.framework.common.bridge.world

import plutoproject.framework.common.api.bridge.Bridge
import plutoproject.framework.common.api.bridge.server.BridgeServer
import plutoproject.framework.common.api.bridge.world.BridgeLocation
import plutoproject.framework.common.api.bridge.world.BridgeWorld
import plutoproject.framework.common.bridge.throwServerNotFound
import plutoproject.framework.common.bridge.throwWorldNotFound
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.LocationInfo
import plutoproject.framework.proto.bridge.locationInfo

fun LocationInfo.createBridge(server: BridgeServer? = null, world: BridgeWorld? = null): BridgeLocation {
    val actualServer = server
        ?: Bridge.getServer(this.server)
        ?: throwServerNotFound(this.server)
    val actualWorld = world
        ?: actualServer.getWorld(this.world)
        ?: throwWorldNotFound(this.world, this.server)
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
