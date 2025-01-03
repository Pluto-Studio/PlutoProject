package plutoproject.framework.paper.bridge.handlers.server

import plutoproject.framework.common.bridge.debugInfo
import plutoproject.framework.common.bridge.internalBridge
import plutoproject.framework.paper.bridge.handlers.NotificationHandler
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.Notification

object ServerInfoUpdateHandler : NotificationHandler {
    override suspend fun handle(request: Notification) {
        if (request.serverInfoUpdate.id == internalBridge.local.id) return
        debugInfo("ServerInfoUpdateHandler: $request")
        internalBridge.syncData(request.serverInfoUpdate)
    }
}
