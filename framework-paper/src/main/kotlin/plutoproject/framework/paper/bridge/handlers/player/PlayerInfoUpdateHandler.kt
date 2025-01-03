package plutoproject.framework.paper.bridge.handlers.player

import plutoproject.framework.common.bridge.debugInfo
import plutoproject.framework.common.bridge.internalBridge
import plutoproject.framework.paper.bridge.handlers.NotificationHandler
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.Notification

object PlayerInfoUpdateHandler : NotificationHandler {
    override suspend fun handle(request: Notification) {
        if (request.playerInfoUpdate.server == internalBridge.local.id) return
        debugInfo("PlayerInfoUpdateHandler: $request")
        internalBridge.updateRemotePlayerInfo(request.playerInfoUpdate)
    }
}
