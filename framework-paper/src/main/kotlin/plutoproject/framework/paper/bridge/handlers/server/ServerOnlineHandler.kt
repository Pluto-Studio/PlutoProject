package plutoproject.framework.paper.bridge.handlers.server

import plutoproject.framework.common.bridge.debugInfo
import plutoproject.framework.common.bridge.internalBridge
import plutoproject.framework.paper.bridge.handlers.NotificationHandler
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.Notification

object ServerOnlineHandler : NotificationHandler {
    override suspend fun handle(request: Notification) {
        if (request.serverOnline == internalBridge.local.id) return
        if (internalBridge.getInternalRemoteServer(request.serverOnline)?.isOnline == true) return
        debugInfo("ServerOnlineHandler: $request")
        internalBridge.markRemoteServerOnline(request.serverOnline)
    }
}
