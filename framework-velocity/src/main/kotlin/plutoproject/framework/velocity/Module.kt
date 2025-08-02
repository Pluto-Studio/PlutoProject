package plutoproject.framework.velocity

import com.github.shynixn.mccoroutine.velocity.registerSuspend
import plutoproject.framework.common.connection.initializeExternalConnections
import plutoproject.framework.common.connection.shutdownExternalConnections
import plutoproject.framework.common.databasepersist.InternalDatabasePersist
import plutoproject.framework.common.util.inject.Koin
import plutoproject.framework.velocity.profile.ProfilePlayerListener
import plutoproject.framework.velocity.util.plugin
import plutoproject.framework.velocity.util.server

fun loadFrameworkModules() {
    initializeExternalConnections()
}

private fun registerListeners() = server.eventManager.apply {
    registerSuspend(plugin, ProfilePlayerListener)
}

private fun registerCommands() {
}

fun enableFrameworkModules() {
    registerListeners()
    registerCommands()
}

fun disableFrameworkModules() {
    Koin.get<InternalDatabasePersist>().close()
    shutdownExternalConnections()
}
