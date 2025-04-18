package plutoproject.framework.common.bridge.player

import kotlinx.coroutines.Deferred
import plutoproject.framework.common.api.bridge.player.PlayerOperationType
import plutoproject.framework.common.api.bridge.world.BridgeLocation
import plutoproject.framework.common.bridge.world.BridgeLocationImpl
import plutoproject.framework.common.bridge.world.createInfo
import plutoproject.framework.common.util.Empty
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperationResult.StatusCase.OK
import plutoproject.framework.proto.bridge.playerOperation
import java.util.*

abstract class RemoteBackendPlayer : RemotePlayer() {
    override val location: Deferred<Result<BridgeLocation>>
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
                    wrapProtoResult(
                        PlayerOperationType.INFO_LOOKUP,
                        result,
                        BridgeLocationImpl(server, world!!, loc.x, loc.y, loc.z, loc.yaw, loc.pitch)
                    )
                }

                else -> wrapProtoResult(PlayerOperationType.INFO_LOOKUP, result, null)
            }
        }

    override suspend fun teleport(location: BridgeLocation): Result<Unit> {
        val result = operatePlayer(playerOperation {
            id = UUID.randomUUID().toString()
            executor = location.server.id
            playerUuid = uniqueId.toString()
            teleport = location.createInfo()
        })
        return wrapProtoResult(PlayerOperationType.TELEPORT, result, Unit)
    }

    override suspend fun performCommand(command: String): Result<Unit> {
        val result = operatePlayer(playerOperation {
            id = UUID.randomUUID().toString()
            executor = server.id
            playerUuid = uniqueId.toString()
            performCommand = command
        })
        return wrapProtoResult(PlayerOperationType.PERFORM_COMMAND, result, Unit)
    }

    override suspend fun switchServer(server: String): Result<Unit> {
        val result = operatePlayer(playerOperation {
            id = UUID.randomUUID().toString()
            executor = this@RemoteBackendPlayer.server.id
            playerUuid = uniqueId.toString()
            switchServer = server
        })
        return wrapProtoResult(PlayerOperationType.SWITCH_SERVER, result, Unit)
    }
}
