package ink.pmc.framework.bridge.backend.handlers.server

import ink.pmc.framework.bridge.backend.handlers.NotificationHandler
import ink.pmc.framework.bridge.internalBridge
import ink.pmc.framework.bridge.proto.BridgeRpcOuterClass.Notification

object ServerOnlineHandler : NotificationHandler {
    override suspend fun handle(request: Notification) {
        if (request.serverOnline == internalBridge.local.id) return
        internalBridge.markServerOnline(request.serverOnline)
    }
}