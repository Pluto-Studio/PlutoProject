package plutoproject.framework.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import plutoproject.framework.common.api.provider.Provider
import plutoproject.framework.common.databasepersist.InternalDatabasePersist
import plutoproject.framework.common.util.inject.Koin
import plutoproject.framework.paper.api.interactive.GuiManager
import plutoproject.framework.paper.interactive.GuiListener
import plutoproject.framework.paper.interactive.commands.InteractiveCommand
import plutoproject.framework.paper.interactive.inventory.InventoryListener
import plutoproject.framework.paper.util.command.AnnotationParser
import plutoproject.framework.paper.util.hook.initHooks
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

fun loadFrameworkModules() {
    Provider
}

fun enableFrameworkModules() {
    initHooks()
    registerListeners()
    registerCommands()
}

private fun registerListeners() = server.pluginManager.apply {
    registerSuspendingEvents(GuiListener, plugin)
    registerSuspendingEvents(InventoryListener, plugin)
}

private fun registerCommands() {
    AnnotationParser.apply {
        parse(InteractiveCommand)
    }
}

fun disableFrameworkModules() {
    Koin.get<InternalDatabasePersist>().close()
    GuiManager.disposeAll()
    Provider.close()
}
