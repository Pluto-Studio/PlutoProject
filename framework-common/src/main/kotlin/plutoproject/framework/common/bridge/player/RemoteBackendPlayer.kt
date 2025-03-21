package plutoproject.framework.common.bridge.player

import kotlinx.coroutines.Deferred
import plutoproject.framework.common.api.bridge.ResultWrapper
import plutoproject.framework.common.api.bridge.world.BridgeLocation
import plutoproject.framework.common.bridge.exception.PlayerOperationResultWrapper
import plutoproject.framework.common.bridge.world.BridgeLocationImpl
import plutoproject.framework.common.bridge.world.createInfo
import plutoproject.framework.common.util.Empty
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperationResult
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperationResult.StatusCase.OK
import plutoproject.framework.proto.bridge.playerOperation
import java.util.*

abstract class RemoteBackendPlayer : RemotePlayer() {
    override val location: Deferred<ResultWrapper<BridgeLocation>>
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
                    PlayerOperationResultWrapper(
                        BridgeLocationImpl(server, world!!, loc.x, loc.y, loc.z, loc.yaw, loc.pitch),
                        result.statusCase, name, server.id, false
                    )
                }

                else -> PlayerOperationResultWrapper(null, result.statusCase, name, server.id, true)
            }
        }

    private fun wrapResultWithoutValue(result: PlayerOperationResult): ResultWrapper<Unit> =
        PlayerOperationResultWrapper(Unit, result.statusCase!!, name, server.id, result.statusCase!! != OK)

    override suspend fun teleport(location: BridgeLocation): ResultWrapper<Unit> {
        val result = operatePlayer(playerOperation {
            id = UUID.randomUUID().toString()
            executor = location.server.id
            playerUuid = uniqueId.toString()
            teleport = location.createInfo()
        })
        return wrapResultWithoutValue(result)
    }

    override suspend fun performCommand(command: String): ResultWrapper<Unit> {
        val result = operatePlayer(playerOperation {
            id = UUID.randomUUID().toString()
            executor = server.id
            playerUuid = uniqueId.toString()
            performCommand = command
        })
        return wrapResultWithoutValue(result)
    }

    override suspend fun switchServer(server: String): ResultWrapper<Unit> {
        val result = operatePlayer(playerOperation {
            id = UUID.randomUUID().toString()
            executor = this@RemoteBackendPlayer.server.id
            playerUuid = uniqueId.toString()
            switchServer = server
        })
        return wrapResultWithoutValue(result)
    }
}
