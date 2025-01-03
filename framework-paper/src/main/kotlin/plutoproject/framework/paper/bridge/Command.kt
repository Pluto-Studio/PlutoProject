package plutoproject.framework.paper.bridge

import plutoproject.framework.common.api.bridge.command.parsers.BridgePlayerNotFoundException
import plutoproject.framework.common.api.bridge.command.parsers.BridgeServerNotFoundException
import plutoproject.framework.paper.api.bridge.command.handlers.BridgePlayerNotFoundHandler
import plutoproject.framework.paper.api.bridge.command.handlers.BridgeServerNotFoundHandler
import plutoproject.framework.paper.util.command.PlatformCommandManager

fun PlatformCommandManager.registerBridgeExceptionHandlers() {
    exceptionController().apply {
        registerHandler(BridgePlayerNotFoundException::class.java, BridgePlayerNotFoundHandler)
        registerHandler(BridgeServerNotFoundException::class.java, BridgeServerNotFoundHandler)
    }
}
