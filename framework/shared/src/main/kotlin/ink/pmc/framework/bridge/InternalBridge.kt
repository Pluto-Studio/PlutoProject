package ink.pmc.framework.bridge

import ink.pmc.framework.bridge.player.InternalPlayer
import ink.pmc.framework.bridge.proto.BridgeRpcOuterClass.*
import ink.pmc.framework.bridge.server.*
import ink.pmc.framework.bridge.world.InternalWorld
import ink.pmc.framework.bridge.world.RemoteBackendWorld
import ink.pmc.framework.bridge.world.createBridge
import ink.pmc.framework.utils.player.uuid
import org.koin.java.KoinJavaComponent.getKoin
import java.util.*

val internalBridge: InternalBridge
    get() = getKoin().get<Bridge>() as InternalBridge

abstract class InternalBridge : Bridge {
    override val servers: MutableSet<BridgeServer> = mutableSetOf()

    private fun unregisterRemoteServer(id: String) {
        debugInfo("InternalBridge - unregisterRemoteServer called: $id")
        require(id != local.id) { "Cannot unregister local server" }
        val remoteServer = getInternalRemoteServer(id) ?: remoteServerNotFound(id)
        remoteServer.isOnline = false
        if (!internalBridge.local.type.isProxy) {
            remoteServer.players.forEach { (it as InternalPlayer).isOnline = false }
        }
        remoteServer.players.clear()
        if (!remoteServer.type.isProxy) {
            remoteServer.worlds.clear()
        }
        servers.remove(remoteServer)
    }

    abstract fun createRemotePlayer(info: PlayerInfo, server: InternalServer? = null): InternalPlayer

    fun createRemoteWorld(info: WorldInfo, server: InternalServer? = null): InternalWorld {
        val actualServer = server ?: getInternalRemoteServer(info.server) ?: remoteServerNotFound(info.server)
        return RemoteBackendWorld(actualServer, info.name, info.alias).apply {
            spawnPoint = info.spawnPoint.createBridge(actualServer, this)
        }
    }

    fun getInternalRemoteServer(id: String): InternalServer? {
        val server = getServer(id) as InternalServer? ?: return null
        if (server.isLocal) return null
        return server
    }

    fun getInternalRemoteBackendPlayer(uniqueId: UUID): InternalPlayer? {
        return getPlayer(uniqueId, ServerState.REMOTE, ServerType.BACKEND) as InternalPlayer?
    }

    fun getInternalLocalPlayer(uniqueId: UUID): InternalPlayer? {
        return getPlayer(uniqueId, ServerState.LOCAL, local.type) as InternalPlayer?
    }

    fun getInternalRemoteWorld(server: BridgeServer, name: String): InternalWorld? {
        val world = server.getWorld(name) as InternalWorld? ?: return null
        if (world.serverState.isLocal) return null
        return world
    }

    fun InternalServer.setInitialPlayers(info: ServerInfo, server: InternalServer) {
        players.clear()
        players.addAll(info.playersList.map {
            createRemotePlayer(it, server)
        })
    }

    fun InternalServer.setInitialWorlds(info: ServerInfo, server: InternalServer) {
        worlds.clear()
        worlds.addAll(info.worldsList.map {
            createRemoteWorld(it, server)
        })
    }

    open fun createRemoteServer(info: ServerInfo): InternalServer {
        val id = info.id
        val group = info.group?.let { getGroup(it) ?: BridgeGroupImpl(it) }
        val server = RemoteBackendServer(id, group).apply {
            setInitialPlayers(info, this)
            setInitialWorlds(info, this)
        }
        return server
    }

    fun registerRemoteServer(info: ServerInfo): BridgeServer {
        debugInfo("InternalBridge - registerRemoteServer called: $info")
        val id = info.id
        if (isServerRegistered(id)) {
            unregisterRemoteServer(id)
        }
        val server = createRemoteServer(info)
        servers.add(server)
        return server
    }

    fun markRemoteServerOffline(id: String) {
        debugInfo("InternalBridge - markRemoteServerOffline called: $id")
        val remoteServer = getInternalRemoteServer(id) ?: remoteServerNotFound(id)
        remoteServer.isOnline = false
        remoteServer.players.forEach { (it as InternalPlayer).isOnline = false }
        remoteServer.players.clear()
        if (!remoteServer.type.isProxy) {
            remoteServer.worlds.clear()
        }
    }

    fun markRemoteServerOnline(id: String) {
        debugInfo("InternalBridge - markRemoteServerOnline called: $id")
        val remoteServer = getInternalRemoteServer(id) ?: remoteServerNotFound(id)
        remoteServer.isOnline = true
    }

    fun syncData(info: ServerInfo): InternalServer {
        debugInfo("InternalBridge - syncData called: $info")
        val remoteServer = getInternalRemoteServer(info.id) ?: remoteServerNotFound(info.id)
        remoteServer.players.forEach { (it as InternalPlayer).isOnline = false }
        remoteServer.setInitialPlayers(info, remoteServer)
        if (!remoteServer.type.isProxy) {
            remoteServer.setInitialWorlds(info, remoteServer)
        }
        return remoteServer
    }

    fun addRemotePlayer(info: PlayerInfo): InternalPlayer {
        debugInfo("InternalBridge - addRemotePlayer called: $info")
        val remotePlayer = createRemotePlayer(info)
        (remotePlayer.server as InternalServer).players.add(remotePlayer)
        return remotePlayer
    }

    fun updateRemotePlayerInfo(info: PlayerInfo): InternalPlayer {
        debugInfo("InternalBridge - updateRemotePlayerInfo called: $info")
        val remotePlayer = getInternalRemoteBackendPlayer(info.uniqueId.uuid) ?: remotePlayerNotFound(info.name)
        remotePlayer.world = getInternalRemoteWorld(remotePlayer.server, info.world.name)
        return remotePlayer
    }

    fun remotePlayerSwitchServer(info: PlayerSwitchServer) {
        debugInfo("InternalBridge - remotePlayerSwitchServer called: $info")
        val remotePlayer = getInternalRemoteBackendPlayer(info.playerUuid.uuid) ?: remotePlayerNotFound(info.playerUuid)
        val target = getInternalRemoteServer(info.server) ?: remoteServerNotFound(info.server)
        target.players.add(remotePlayer)
        remotePlayer.server = target
        remotePlayer.world = null
        (remotePlayer.server as InternalServer).players.remove(remotePlayer)
    }

    fun removeRemotePlayers(uuid: UUID) {
        debugInfo("InternalBridge - removeRemotePlayers called: $uuid")
        servers.flatMap { it.players }.filter { it.uniqueId == uuid && it.serverState.isRemote }.forEach {
            it as InternalPlayer
            it.isOnline = false
            (it.server as InternalServer).players.remove(it)
        }
    }

    fun removeRemoteBackendPlayer(uuid: UUID) {
        debugInfo("InternalBridge - removeRemoteBackendPlayer called: $uuid")
        val remotePlayer = getInternalRemoteBackendPlayer(uuid) ?: remotePlayerNotFound(uuid.toString())
        remotePlayer.isOnline = false
        (remotePlayer.server as InternalServer).players.remove(remotePlayer)
    }

    fun addRemoteWorld(info: WorldInfo): InternalWorld {
        debugInfo("InternalBridge - addRemoteWorld called: $info")
        val remoteWorld = createRemoteWorld(info)
        require(!remoteWorld.server.type.isProxy) { "Cannot add world to proxy server" }
        (remoteWorld.server as InternalServer).worlds.add(remoteWorld)
        return remoteWorld
    }

    fun updateRemoteWorldInfo(info: WorldInfo): InternalWorld {
        debugInfo("InternalBridge - updateRemoteWorldInfo called: $info")
        val remoteServer = getInternalRemoteServer(info.server) ?: remoteServerNotFound(info.server)
        val remoteWorld = getInternalRemoteWorld(remoteServer, info.name) ?: remoteWorldNotFound(info.name, info.server)
        remoteWorld.spawnPoint = info.spawnPoint.createBridge()
        return remoteWorld
    }

    fun removeRemoteWorld(load: WorldLoad) {
        debugInfo("InternalBridge - removeRemoteWorld called: $load")
        val remoteServer = getInternalRemoteServer(load.server) ?: remoteServerNotFound(load.server)
        require(!remoteServer.type.isProxy) { "Cannot remove world from proxy server" }
        val remoteWorld = getInternalRemoteWorld(remoteServer, load.world)
            ?: remoteWorldNotFound(load.world, load.server)
        remoteServer.worlds.remove(remoteWorld)
    }
}