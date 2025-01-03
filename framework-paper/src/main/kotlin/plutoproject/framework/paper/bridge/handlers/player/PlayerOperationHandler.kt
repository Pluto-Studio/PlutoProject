package plutoproject.framework.paper.bridge.handlers.player

import ink.pmc.advkt.sound.key
import ink.pmc.advkt.sound.pitch
import ink.pmc.advkt.sound.source
import ink.pmc.advkt.sound.volume
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import plutoproject.framework.common.bridge.*
import plutoproject.framework.common.bridge.player.createInfoWithoutLocation
import plutoproject.framework.common.bridge.world.createInfo
import plutoproject.framework.common.util.data.convertToUuid
import plutoproject.framework.common.util.time.currentTimestampMillis
import plutoproject.framework.paper.bridge.bridgeStub
import plutoproject.framework.paper.bridge.handlers.NotificationHandler
import plutoproject.framework.paper.bridge.server.localServer
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.Notification
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperation.ContentCase.*
import plutoproject.framework.proto.bridge.playerOperationAck

object PlayerOperationHandler : NotificationHandler {
    override suspend fun handle(request: Notification) {
        val msg = request.playerOperation
        val playerUuid = request.playerOperation.playerUuid.convertToUuid()
        if (msg.executor != internalBridge.local.id) return
        val localPlayer = internalBridge.getInternalLocalPlayer(playerUuid)
            ?: return warn { throwLocalPlayerNotFound(playerUuid.toString()) }
        debugInfo("PlayerOperationHandler: $request, $currentTimestampMillis")
        when (msg.contentCase!!) {
            INFO_LOOKUP -> {
                val result = bridgeStub.ackPlayerOperation(playerOperationAck {
                    id = request.playerOperation.id
                    ok = true
                    infoLookup = localPlayer.createInfoWithoutLocation().toBuilder().apply {
                        location = localPlayer.location.await().createInfo()
                    }.build()
                })
                checkCommonResult(result)
                return
            }

            SEND_MESSAGE -> error("Unexpected")
            SHOW_TITLE -> error("Unexpected")
            PLAY_SOUND -> {
                val info = request.playerOperation.playSound
                localPlayer.playSound {
                    key(Key.key(info.key))
                    source(Sound.Source.valueOf(info.source))
                    volume(info.volume)
                    pitch(info.pitch)
                }
            }

            TELEPORT -> {
                val location = localServer.getWorld(msg.teleport.world)?.getLocation(
                    msg.teleport.x,
                    msg.teleport.y,
                    msg.teleport.z,
                    msg.teleport.yaw,
                    msg.teleport.pitch,
                ) ?: return warn { throwLocalWorldNotFound(msg.teleport.world) }
                localPlayer.teleport(location)
            }

            PERFORM_COMMAND -> localPlayer.performCommand(msg.performCommand)
            CONTENT_NOT_SET -> warn { throwContentNotSet("PlayerOperation") }
            SWITCH_SERVER -> error("Unexpected")
        }
        val result = bridgeStub.ackPlayerOperation(playerOperationAck {
            id = request.playerOperation.id
            ok = true
        })
        checkCommonResult(result)
    }
}
