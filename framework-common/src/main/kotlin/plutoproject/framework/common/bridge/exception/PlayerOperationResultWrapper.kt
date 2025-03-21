package plutoproject.framework.common.bridge.exception

import plutoproject.framework.common.bridge.*
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperationResult
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperationResult.StatusCase.*

class PlayerOperationResultWrapper<T>(
    override val value: T?,
    override val state: PlayerOperationResult.StatusCase,
    private val player: String,
    private val server: String,
    override val isExceptionOccurred: Boolean
) : StatefulResultWrapper<T, PlayerOperationResult.StatusCase> {
    override fun throwException(): Nothing {
        when (state) {
            OK -> error("Unexpected")
            PLAYER_OFFLINE -> throwPlayerNotFound(player)
            SERVER_OFFLINE -> throwServerNotFound(server)
            WORLD_NOT_FOUND -> throwWorldNotFound()
            TIMEOUT -> throwPlayerOperationTimeout(player)
            UNSUPPORTED -> error("Unsupported")
            MISSING_FIELDS -> throwMissingFields()
            STATUS_NOT_SET -> throwStatusNotSet("PlayerOperationResult")
        }
    }
}
