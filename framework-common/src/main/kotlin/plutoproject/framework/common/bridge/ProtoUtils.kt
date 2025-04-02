package plutoproject.framework.common.bridge

import plutoproject.framework.common.api.bridge.player.PlayerOperationType
import plutoproject.framework.common.api.bridge.server.WorldOperationType
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.PlayerOperation
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.WorldOperation

val PlayerOperation.type: PlayerOperationType
    get() = when {
        hasInfoLookup() -> PlayerOperationType.INFO_LOOKUP
        hasSendMessage() -> PlayerOperationType.SEND_MESSAGE
        hasShowTitle() -> PlayerOperationType.SHOW_TITLE
        hasPlaySound() -> PlayerOperationType.PLAY_SOUND
        hasTeleport() -> PlayerOperationType.TELEPORT
        hasPerformCommand() -> PlayerOperationType.PERFORM_COMMAND
        hasSwitchServer() -> PlayerOperationType.SWITCH_SERVER
        else -> error("Unknown player operation type?")
    }

val WorldOperation.type: WorldOperationType
    get() = when {
        hasPlaceholder() -> WorldOperationType.PLACEHOLDER
        else -> error("Unknown world operation type?")
    }
