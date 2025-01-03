package plutoproject.framework.common.api.bridge.world

import plutoproject.framework.common.api.bridge.server.BridgeServer

interface BridgeLocation {
    val server: BridgeServer
    val world: BridgeWorld
    val x: Double
    val y: Double
    val z: Double
    val yaw: Float
    val pitch: Float
}
