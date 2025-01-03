package plutoproject.framework.paper.bridge.handlers.world

import plutoproject.framework.common.bridge.debugInfo
import plutoproject.framework.common.bridge.internalBridge
import plutoproject.framework.paper.bridge.handlers.NotificationHandler
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass

object WorldOperationHandler : NotificationHandler {
    override suspend fun handle(request: BridgeRpcOuterClass.Notification) {
        if (request.worldOperation.executor != internalBridge.local.id) return
        debugInfo("WorldOperationHandler: $request")
    }
}
