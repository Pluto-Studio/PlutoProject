package ink.pmc.framework.bridge.world

import ink.pmc.framework.bridge.Bridge
import ink.pmc.framework.bridge.proto.BridgeRpcOuterClass.WorldInfo
import ink.pmc.framework.bridge.proto.worldInfo

fun BridgeWorld.createInfo(): WorldInfo {
    val world = this
    return worldInfo {
        this.server = world.server.id
        name = world.name
        world.alias?.also { alias = it }
        spawnPoint = world.spawnPoint.createInfo()
    }
}

internal fun WorldInfo.getBridge(): BridgeWorld? {
    val remoteServer = Bridge.getServer(server) ?: return null
    return Bridge.getWorld(remoteServer, name)
}

abstract class InternalWorld : BridgeWorld {
    abstract override var spawnPoint: BridgeLocation

    fun update(info: WorldInfo) {
        spawnPoint = info.spawnPoint.createBridge()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is BridgeWorld) return false
        return other.server == server
                && other.name == name
                && other.serverState == serverState
                && other.serverType == serverType
    }

    override fun hashCode(): Int {
        var result = server.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + serverState.hashCode()
        result = 31 * result + serverType.hashCode()
        return result
    }
}