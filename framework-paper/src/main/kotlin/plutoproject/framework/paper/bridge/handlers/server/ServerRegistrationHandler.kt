package plutoproject.framework.paper.bridge.handlers.server

import plutoproject.framework.common.bridge.debugInfo
import plutoproject.framework.common.bridge.internalBridge
import plutoproject.framework.paper.bridge.handlers.NotificationHandler
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.Notification

object ServerRegistrationHandler : NotificationHandler {
    override suspend fun handle(request: Notification) {
        if (request.serverRegistration.id == internalBridge.local.id) return
        debugInfo("ServerRegistrationHandler: $request")
        internalBridge.registerRemoteServer(request.serverRegistration)
    }
}
