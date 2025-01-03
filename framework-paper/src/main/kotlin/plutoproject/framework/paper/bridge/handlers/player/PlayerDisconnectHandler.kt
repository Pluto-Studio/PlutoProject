package plutoproject.framework.paper.bridge.handlers.player

import plutoproject.framework.common.bridge.debugInfo
import plutoproject.framework.common.bridge.internalBridge
import plutoproject.framework.common.util.data.convertToUuid
import plutoproject.framework.paper.bridge.handlers.NotificationHandler
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.Notification

object PlayerDisconnectHandler : NotificationHandler {
    override suspend fun handle(request: Notification) {
        debugInfo("PlayerDisconnectHandler: $request")
        internalBridge.removeRemotePlayers(request.playerDisconnect.uniqueId.convertToUuid())
    }
}
