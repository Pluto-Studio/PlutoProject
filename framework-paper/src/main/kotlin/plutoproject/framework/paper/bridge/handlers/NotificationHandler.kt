package plutoproject.framework.paper.bridge.handlers

import plutoproject.framework.paper.bridge.handlers.player.*
import plutoproject.framework.paper.bridge.handlers.server.ServerInfoUpdateHandler
import plutoproject.framework.paper.bridge.handlers.server.ServerOfflineHandler
import plutoproject.framework.paper.bridge.handlers.server.ServerOnlineHandler
import plutoproject.framework.paper.bridge.handlers.server.ServerRegistrationHandler
import plutoproject.framework.paper.bridge.handlers.world.WorldInfoUpdateHandler
import plutoproject.framework.paper.bridge.handlers.world.WorldLoadHandler
import plutoproject.framework.paper.bridge.handlers.world.WorldOperationHandler
import plutoproject.framework.paper.bridge.handlers.world.WorldUnloadHandler
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.Notification
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.Notification.ContentCase
import plutoproject.framework.proto.bridge.BridgeRpcOuterClass.Notification.ContentCase.*

interface NotificationHandler {
    companion object {
        private val handlers = mapOf(
            SERVER_REGISTRATION to ServerRegistrationHandler,
            SERVER_INFO_UPDATE to ServerInfoUpdateHandler,
            SERVER_OFFLINE to ServerOfflineHandler,
            SERVER_ONLINE to ServerOnlineHandler,

            PLAYER_OPERATION to PlayerOperationHandler,
            PLAYER_INFO_UPDATE to PlayerInfoUpdateHandler,
            PLAYER_SWITCH_SERVER to PlayerSwitchServerHandler,
            PLAYER_JOIN to PlayerJoinHandler,
            PLAYER_DISCONNECT to PlayerDisconnectHandler,

            WORLD_OPERATION to WorldOperationHandler,
            WORLD_INFO_UPDATE to WorldInfoUpdateHandler,
            WORLD_LOAD to WorldLoadHandler,
            WORLD_UNLOAD to WorldUnloadHandler,
        )

        operator fun get(type: ContentCase): NotificationHandler {
            return handlers[type] ?: error("Handler for $type not found")
        }
    }

    suspend fun handle(request: Notification)
}
