package plutoproject.framework.common.bridge.player

import kotlinx.coroutines.Deferred
import plutoproject.framework.common.api.bridge.world.BridgeLocation
import plutoproject.framework.common.bridge.throwRemoteWorldNotFound
import plutoproject.framework.common.bridge.warn
import plutoproject.framework.common.bridge.world.BridgeLocationImpl
import plutoproject.framework.common.bridge.world.createInfo
import plutoproject.framework.common.util.Empty
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperationResult
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperationResult.StatusCase.*
import plutoproject.framework.proto.bridge.playerOperation
import java.util.*

abstract class RemoteBackendPlayer : RemotePlayer() {
    override val location: Deferred<BridgeLocation>
        get() = runAsync {
            val result = operatePlayer(playerOperation {
                id = UUID.randomUUID().toString()
                executor = server.id
                playerUuid = uniqueId.toString()
                infoLookup = Empty
            })
            val info = result.infoLookup
            when (result.statusCase!!) {
                OK -> {
                    check(info.hasLocation()) { "PlayerInfo missing required field" }
                    val loc = info.location
                    BridgeLocationImpl(server, world!!, loc.x, loc.y, loc.z, loc.yaw, loc.pitch)
                }

                else -> throw checkNoReturnResult(result) ?: error("Unexpected")
            }
        }


    private fun checkNoReturnResult(result: PlayerOperationResult): IllegalStateException? {
        return when (result.statusCase!!) {
            OK -> null
            PLAYER_OFFLINE -> error("Player offline: $name")
            SERVER_OFFLINE -> error("Remote server offline: ${server.id}")
            WORLD_NOT_FOUND -> throwRemoteWorldNotFound("${world?.name}", server.id)
            UNSUPPORTED -> error("Unsupported")
            TIMEOUT -> error("Player operation timeout: $name")
            MISSING_FIELDS -> error("Missing fields")
            STATUS_NOT_SET -> error("Received a PlayerOperationResult without status (player: $name")
        }
    }

    override suspend fun teleport(location: BridgeLocation) {
        val result = operatePlayer(playerOperation {
            id = UUID.randomUUID().toString()
            executor = location.server.id
            playerUuid = uniqueId.toString()
            teleport = location.createInfo()
        })
        checkNoReturnResult(result)?.let { warn { throw it } }
    }

    override suspend fun performCommand(command: String) {
        val result = operatePlayer(playerOperation {
            id = UUID.randomUUID().toString()
            executor = server.id
            playerUuid = uniqueId.toString()
            performCommand = command
        })
        checkNoReturnResult(result)?.let { warn { throw it } }
    }

    override suspend fun switchServer(server: String) {
        val result = operatePlayer(playerOperation {
            id = UUID.randomUUID().toString()
            executor = this@RemoteBackendPlayer.server.id
            playerUuid = uniqueId.toString()
            switchServer = server
        })
        checkNoReturnResult(result)?.let { warn { throw it } }
    }
}
