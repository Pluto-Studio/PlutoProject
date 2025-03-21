package plutoproject.framework.common.bridge.player

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import plutoproject.framework.common.api.bridge.ResultWrapper
import plutoproject.framework.common.bridge.exception.PlayerOperationResultWrapper
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperation
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperationResult
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperationResult.StatusCase.OK
import plutoproject.framework.proto.bridge.playerOperation
import plutoproject.framework.proto.bridge.soundInfo
import plutoproject.framework.proto.bridge.titleInfo
import java.util.*

abstract class RemotePlayer : InternalPlayer() {
    protected fun wrapResultWithoutValue(result: PlayerOperationResult): ResultWrapper<Unit> =
        PlayerOperationResultWrapper(Unit, result.statusCase!!, name, server.id, result.statusCase!! != OK)

    override suspend fun sendMessage(message: Component): ResultWrapper<Unit> {
        val result = operatePlayer(playerOperation {
            id = UUID.randomUUID().toString()
            executor = server.id
            playerUuid = uniqueId.toString()
            sendMessage = MiniMessage.miniMessage().serialize(message)
        })
        return wrapResultWithoutValue(result)
    }

    override suspend fun showTitle(title: Title): ResultWrapper<Unit> {
        val result = operatePlayer(playerOperation {
            id = UUID.randomUUID().toString()
            executor = server.id
            playerUuid = uniqueId.toString()
            showTitle = titleInfo {
                val times = title.times()
                fadeInMs = times?.fadeIn()?.toMillis() ?: 500
                stayMs = times?.stay()?.toMillis() ?: 3500
                fadeOutMs = times?.fadeOut()?.toMillis() ?: 1000
                mainTitle = MiniMessage.miniMessage().serialize(title.title())
                subTitle = MiniMessage.miniMessage().serialize(title.subtitle())
            }
        })
        return wrapResultWithoutValue(result)
    }

    override suspend fun playSound(sound: Sound): ResultWrapper<Unit> {
        val result = operatePlayer(playerOperation {
            id = UUID.randomUUID().toString()
            executor = server.id
            playerUuid = uniqueId.toString()
            playSound = soundInfo {
                key = sound.name().asMinimalString()
                source = sound.source().toString()
                volume = sound.volume()
                pitch = sound.pitch()
            }
        })
        return wrapResultWithoutValue(result)
    }

    abstract suspend fun operatePlayer(request: PlayerOperation): PlayerOperationResult
}
