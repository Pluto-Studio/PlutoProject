package plutoproject.framework.common.api.bridge.server

interface ServerLookup {
    val servers: Collection<BridgeServer>

    fun getServer(id: String): BridgeServer? {
        return servers.firstOrNull { it.id == id }
    }

    fun isServerRegistered(id: String): Boolean {
        return getServer(id) != null
    }
}
