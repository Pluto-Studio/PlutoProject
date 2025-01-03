package plutoproject.framework.paper.bridge.handlers.player

import plutoproject.framework.common.bridge.debugInfo
import plutoproject.framework.common.bridge.internalBridge
import plutoproject.framework.paper.bridge.handlers.NotificationHandler
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.Notification

object PlayerJoinHandler : NotificationHandler {
    override suspend fun handle(request: Notification) {
        if (request.playerJoin.server == internalBridge.local.id) return
        debugInfo("PlayerJoinHandler: $request")
        internalBridge.addRemotePlayer(request.playerJoin)
    }
}
