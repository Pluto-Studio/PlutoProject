package plutoproject.framework.paper.bridge.handlers.world

import plutoproject.framework.common.bridge.debugInfo
import plutoproject.framework.common.bridge.internalBridge
import plutoproject.framework.paper.bridge.handlers.NotificationHandler
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.Notification

object WorldInfoUpdateHandler : NotificationHandler {
    override suspend fun handle(request: Notification) {
        if (request.worldInfoUpdate.server == internalBridge.local.id) return
        debugInfo("WorldInfoUpdateHandler: $request")
        internalBridge.updateRemoteWorldInfo(request.worldInfoUpdate)
    }
}
