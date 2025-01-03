package plutoproject.framework.paper.bridge.handlers.server

import plutoproject.framework.common.bridge.debugInfo
import plutoproject.framework.common.bridge.internalBridge
import plutoproject.framework.paper.bridge.handlers.NotificationHandler
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.Notification

object ServerOfflineHandler : NotificationHandler {
    override suspend fun handle(request: Notification) {
        if (request.serverOffline == internalBridge.local.id) return
        if (internalBridge.getInternalRemoteServer(request.serverOffline)?.isOnline == false) return
        debugInfo("ServerOfflineHandler: $request")
        internalBridge.markRemoteServerOffline(request.serverOffline)
    }
}
