package plutoproject.framework.velocity.bridge

import plutoproject.framework.common.api.bridge.command.parsers.BridgePlayerNotFoundException
import plutoproject.framework.common.api.bridge.command.parsers.BridgeServerNotFoundException
import plutoproject.framework.velocity.api.bridge.command.handlers.BridgePlayerNotFoundHandler
import plutoproject.framework.velocity.api.bridge.command.handlers.BridgeServerNotFoundHandler
import plutoproject.framework.velocity.util.command.PlatformCommandManager

fun PlatformCommandManager.registerBridgeExceptionHandlers() {
    exceptionController().apply {
        registerHandler(BridgePlayerNotFoundException::class.java, BridgePlayerNotFoundHandler)
        registerHandler(BridgeServerNotFoundException::class.java, BridgeServerNotFoundHandler)
    }
}
