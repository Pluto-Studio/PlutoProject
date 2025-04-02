package plutoproject.framework.common.bridge.player

import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import plutoproject.framework.common.api.bridge.*
import plutoproject.framework.common.api.bridge.player.PlayerOperationType
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperation
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperationResult
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperationResult.StatusCase.*
import plutoproject.framework.proto.bridge.playerOperation
import plutoproject.framework.proto.bridge.soundInfo
import plutoproject.framework.proto.bridge.titleInfo
import java.util.*

abstract class RemotePlayer : InternalPlayer() {
    protected fun <T> wrapProtoResult(type: PlayerOperationType, result: PlayerOperationResult, value: T?): Result<T> {
        return when (result.statusCase!!) {
            OK -> value?.let { Result.success(it) } ?: error("Player operation succeed but has no valid value?")
            PLAYER_OFFLINE -> Result.failure(PlayerOfflineException(uniqueId))
            SERVER_OFFLINE -> Result.failure(ServerOfflineException(server.id))
            WORLD_NOT_FOUND -> world?.let { Result.failure(WorldNotFoundException(it.name)) }
                ?: error("Player $name's world is not initialized")

            TIMEOUT -> Result.failure(PlayerOperationTimeoutException(uniqueId, type))
            UNSUPPORTED -> Result.failure(UnsupportedException())
            MISSING_FIELDS -> error("Missing fields in PlayerOperation: $name")
            STATUS_NOT_SET -> error("Status not set in PlayerOperation: $name")
        }
    }

    override suspend fun sendMessage(message: Component): Result<Unit> {
        val result = operatePlayer(playerOperation {
            id = UUID.randomUUID().toString()
            executor = server.id
            playerUuid = uniqueId.toString()
            sendMessage = MiniMessage.miniMessage().serialize(message)
        })
        return wrapProtoResult(PlayerOperationType.SEND_MESSAGE, result, Unit)
    }

    override suspend fun showTitle(title: Title): Result<Unit> {
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
        return wrapProtoResult(PlayerOperationType.SHOW_TITLE, result, Unit)
    }

    override suspend fun playSound(sound: Sound): Result<Unit> {
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
        return wrapProtoResult(PlayerOperationType.PLAY_SOUND, result, Unit)
    }

    abstract suspend fun operatePlayer(request: PlayerOperation): PlayerOperationResult
}
