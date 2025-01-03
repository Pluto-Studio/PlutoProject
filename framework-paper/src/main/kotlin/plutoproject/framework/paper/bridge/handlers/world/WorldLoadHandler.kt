package plutoproject.framework.paper.bridge.handlers.world

import plutoproject.framework.common.bridge.debugInfo
import plutoproject.framework.common.bridge.internalBridge
import plutoproject.framework.paper.bridge.handlers.NotificationHandler
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.Notification

object WorldLoadHandler : NotificationHandler {
    override suspend fun handle(request: Notification) {
        if (request.worldLoad.server == internalBridge.local.id) return
        debugInfo("WorldLoadHandler: $request")
        internalBridge.addRemoteWorld(request.worldLoad)
    }
}
