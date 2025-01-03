package plutoproject.framework.velocity

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import plutoproject.framework.common.api.bridge.Bridge
import plutoproject.framework.common.api.provider.Provider
import plutoproject.framework.common.api.rpc.RpcServer
import plutoproject.framework.common.bridge.registerBridgeArgumentParsers
import plutoproject.framework.velocity.bridge.BridgeCommand
import plutoproject.framework.velocity.bridge.BridgePlayerListener
import plutoproject.framework.velocity.bridge.BridgeRpc
import plutoproject.framework.velocity.bridge.registerBridgeExceptionHandlers
import plutoproject.framework.velocity.options.OptionsListener
import plutoproject.framework.velocity.options.proto.OptionsRpc
import plutoproject.framework.velocity.playerdb.proto.PlayerDBRpc
import plutoproject.framework.velocity.profile.ProfileCacheListener
import plutoproject.framework.velocity.rpc.RpcCommand
import plutoproject.framework.velocity.util.command.AnnotationParser
import plutoproject.framework.velocity.util.command.CommandManager
import plutoproject.framework.velocity.util.plugin
import plutoproject.framework.velocity.util.server

fun loadFrameworkModules() {
    RpcServer.apply {
        addService(OptionsRpc)
        addService(PlayerDBRpc)
        addService(BridgeRpc)
    }
    Provider
}

private fun registerListeners() = server.eventManager.apply {
    registerSuspend(plugin, OptionsListener)
    registerSuspend(plugin, BridgePlayerListener)
    registerSuspend(plugin, ProfileCacheListener)
}

private fun registerCommands() {
    CommandManager.apply {
        registerBridgeArgumentParsers()
        registerBridgeExceptionHandlers()
    }
    AnnotationParser.apply {
        parse(RpcCommand)
        parse(BridgeCommand)
    }
}

fun enableFrameworkModules() {
    registerListeners()
    registerCommands()
    RpcServer.start()
    Bridge
}

fun disableFrameworkModules() {
    Provider.close()
    RpcServer.stop()
}
