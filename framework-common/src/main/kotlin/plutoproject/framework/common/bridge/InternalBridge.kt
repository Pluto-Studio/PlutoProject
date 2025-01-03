package plutoproject.framework.common.bridge

import org.koin.java.KoinJavaComponent.getKoin
import plutoproject.framework.common.api.bridge.Bridge
import plutoproject.framework.common.api.bridge.server.BridgeServer
import plutoproject.framework.common.api.bridge.server.ServerState
import plutoproject.framework.common.api.bridge.server.ServerType
import plutoproject.framework.common.bridge.player.InternalPlayer
import plutoproject.framework.common.bridge.server.BridgeGroupImpl
import plutoproject.framework.common.bridge.server.InternalServer
import plutoproject.framework.common.bridge.server.RemoteBackendServer
import plutoproject.framework.common.bridge.world.InternalWorld
import plutoproject.framework.common.bridge.world.RemoteBackendWorld
import plutoproject.framework.common.bridge.world.createBridge
import plutoproject.framework.common.util.data.convertToUuid
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.*
import java.util.*

val internalBridge: InternalBridge
    get() = getKoin().get<Bridge>() as InternalBridge

abstract class InternalBridge : Bridge {
    override val servers: MutableSet<BridgeServer> = mutableSetOf()

    private fun unregisterRemoteServer(id: String) {
        debugInfo("InternalBridge - unregisterRemoteServer called: $id")
        require(id != local.id) { "Cannot unregister local server" }
        val remoteServer = getInternalRemoteServer(id)
            ?: return warn { throwRemoteServerNotFound(id) }
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

    abstract fun createRemotePlayer(info: PlayerInfo, server: InternalServer? = null): InternalPlayer?

    private fun createRemoteWorld(info: WorldInfo, server: InternalServer? = null): InternalWorld? {
        val actualServer = server ?: getInternalRemoteServer(info.server)
        if (actualServer == null) {
            warn { throwRemoteServerNotFound(info.server) }
            return null
        }
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
        players.addAll(info.playersList.mapNotNull {
            createRemotePlayer(it, server)
        })
    }

    fun InternalServer.setInitialWorlds(info: ServerInfo, server: InternalServer) {
        worlds.clear()
        worlds.addAll(info.worldsList.mapNotNull {
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

    fun registerRemoteServer(info: ServerInfo): InternalServer {
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
        val remoteServer = getInternalRemoteServer(id)
            ?: return warn { throwRemoteServerNotFound(id) }
        if (!remoteServer.isOnline) return
        remoteServer.isOnline = false
        remoteServer.players.forEach { (it as InternalPlayer).isOnline = false }
        remoteServer.players.clear()
        if (!remoteServer.type.isProxy) {
            remoteServer.worlds.clear()
        }
    }

    fun markRemoteServerOnline(id: String) {
        debugInfo("InternalBridge - markRemoteServerOnline called: $id")
        val remoteServer = getInternalRemoteServer(id)
            ?: return warn { throwRemoteServerNotFound(id) }
        if (remoteServer.isOnline) return
        remoteServer.isOnline = true
    }

    fun syncData(info: ServerInfo): InternalServer {
        debugInfo("InternalBridge - syncData called: $info")
        val remoteServer = getInternalRemoteServer(info.id) ?: return registerRemoteServer(info)
        if (!remoteServer.isOnline) {
            markRemoteServerOnline(remoteServer.id)
        }
        remoteServer.players.forEach { (it as InternalPlayer).isOnline = false }
        if (!remoteServer.type.isProxy) {
            remoteServer.setInitialWorlds(info, remoteServer)
        }
        remoteServer.setInitialPlayers(info, remoteServer)
        return remoteServer
    }

    fun addRemotePlayer(info: PlayerInfo) {
        debugInfo("InternalBridge - addRemotePlayer called: $info")
        val remotePlayer = createRemotePlayer(info) ?: return
        (remotePlayer.server as InternalServer).players.add(remotePlayer)
    }

    fun updateRemotePlayerInfo(info: PlayerInfo) {
        debugInfo("InternalBridge - updateRemotePlayerInfo called: $info")
        val remotePlayer = getInternalRemoteBackendPlayer(info.uniqueId.convertToUuid())
            ?: return warn { throwRemotePlayerNotFound(info.name) }
        remotePlayer.world = getInternalRemoteWorld(remotePlayer.server, info.world.name)
    }

    fun remotePlayerSwitchServer(info: PlayerInfo) {
        debugInfo("InternalBridge - remotePlayerSwitchServer called: $info")
        val remotePlayer = getInternalRemoteBackendPlayer(info.uniqueId.convertToUuid())
            ?: createRemotePlayer(info)
            ?: return
        val target = getInternalRemoteServer(info.server)
            ?: return warn { throwRemoteServerNotFound(info.server) }
        (remotePlayer.server as InternalServer).players.remove(remotePlayer)
        target.players.add(remotePlayer)
        remotePlayer.server = target
        remotePlayer.world = null
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
        val remotePlayer = getInternalRemoteBackendPlayer(uuid)
            ?: return warn { throwRemotePlayerNotFound(uuid.toString()) }
        (remotePlayer.server as InternalServer).players.remove(remotePlayer)
        remotePlayer.isOnline = false
    }

    fun addRemoteWorld(info: WorldInfo) {
        debugInfo("InternalBridge - addRemoteWorld called: $info")
        val remoteWorld = createRemoteWorld(info)
            ?: return
        require(!remoteWorld.server.type.isProxy) { "Cannot add world to proxy server" }
        (remoteWorld.server as InternalServer).worlds.add(remoteWorld)
    }

    fun updateRemoteWorldInfo(info: WorldInfo) {
        debugInfo("InternalBridge - updateRemoteWorldInfo called: $info")
        val remoteServer = getInternalRemoteServer(info.server)
            ?: return warn { throwRemoteServerNotFound(info.server) }
        val remoteWorld = getInternalRemoteWorld(remoteServer, info.name)
            ?: return warn { throwRemoteWorldNotFound(info.name, info.server) }
        remoteWorld.spawnPoint = info.spawnPoint.createBridge()
    }

    fun removeRemoteWorld(load: WorldLoad) {
        debugInfo("InternalBridge - removeRemoteWorld called: $load")
        val remoteServer = getInternalRemoteServer(load.server)
            ?: return warn { throwRemoteServerNotFound(load.server) }
        require(!remoteServer.type.isProxy) { "Cannot remove world from proxy server" }
        val remoteWorld = getInternalRemoteWorld(remoteServer, load.world)
            ?: return warn { throwRemoteWorldNotFound(load.world, load.server) }
        remoteServer.worlds.remove(remoteWorld)
    }
}
