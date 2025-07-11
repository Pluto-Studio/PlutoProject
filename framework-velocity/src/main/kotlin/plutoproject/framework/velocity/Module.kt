package plutoproject.framework.velocity

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import plutoproject.framework.common.api.provider.Provider
import plutoproject.framework.common.api.rpc.RpcServer
import plutoproject.framework.common.databasepersist.InternalDatabasePersist
import plutoproject.framework.common.util.inject.Koin
import plutoproject.framework.velocity.options.OptionsPlayerListener
import plutoproject.framework.velocity.options.proto.OptionsRpc
import plutoproject.framework.velocity.playerdb.proto.PlayerDBRpc
import plutoproject.framework.velocity.profile.ProfilePlayerListener
import plutoproject.framework.velocity.rpc.RpcCommand
import plutoproject.framework.velocity.util.command.AnnotationParser
import plutoproject.framework.velocity.util.plugin
import plutoproject.framework.velocity.util.server

fun loadFrameworkModules() {
    RpcServer.apply {
        addService(OptionsRpc)
        addService(PlayerDBRpc)
    }
    Provider
}

private fun registerListeners() = server.eventManager.apply {
    registerSuspend(plugin, OptionsPlayerListener)
    registerSuspend(plugin, ProfilePlayerListener)
}

private fun registerCommands() {
    AnnotationParser.apply {
        parse(RpcCommand)
    }
}

fun enableFrameworkModules() {
    registerListeners()
    registerCommands()
    RpcServer.start()
}

fun disableFrameworkModules() {
    Koin.get<InternalDatabasePersist>().close()
    Provider.close()
    RpcServer.stop()
}
