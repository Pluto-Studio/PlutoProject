package plutoproject.framework.paper.bridge.handlers.world

import plutoproject.framework.common.bridge.debugInfo
import plutoproject.framework.common.bridge.internalBridge
import plutoproject.framework.paper.bridge.handlers.NotificationHandler
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass

object WorldUnloadHandler : NotificationHandler {
    override suspend fun handle(request: BridgeRpcOuterClass.Notification) {
        if (request.worldUnload.server == internalBridge.local.id) return
        debugInfo("WorldUnloadHandler: $request")
        internalBridge.removeRemoteWorld(request.worldUnload)
    }
}
