package plutoproject.framework.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import plutoproject.framework.common.api.provider.Provider
import plutoproject.framework.common.api.rpc.RpcClient
import plutoproject.framework.paper.api.interactive.GuiManager
import plutoproject.framework.paper.interactive.GuiListener
import plutoproject.framework.paper.interactive.commands.InteractiveCommand
import plutoproject.framework.paper.interactive.inventory.InventoryListener
import plutoproject.framework.paper.options.PaperOptionsListener
import plutoproject.framework.paper.options.startOptionsMonitor
import plutoproject.framework.paper.options.stopOptionsMonitor
import plutoproject.framework.paper.playerdb.startPlayerDBMonitor
import plutoproject.framework.paper.playerdb.stopPlayerDBMonitor
import plutoproject.framework.paper.util.command.AnnotationParser
import plutoproject.framework.paper.util.hook.initHooks
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

fun loadFrameworkModules() {
    RpcClient.start()
    Provider
}

fun enableFrameworkModules() {
    initHooks()
    registerListeners()
    registerCommands()
    startOptionsMonitor()
    startPlayerDBMonitor()
}

private fun registerListeners() = server.pluginManager.apply {
    registerSuspendingEvents(GuiListener, plugin)
    registerSuspendingEvents(InventoryListener, plugin)
    registerSuspendingEvents(PaperOptionsListener, plugin)
}

private fun registerCommands() {
    AnnotationParser.apply {
        parse(InteractiveCommand)
    }
}

fun disableFrameworkModules() {
    stopOptionsMonitor()
    stopPlayerDBMonitor()
    GuiManager.disposeAll()
    Provider.close()
    RpcClient.stop()
}
