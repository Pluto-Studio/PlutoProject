package plutoproject.capability.interactive.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import kotlinx.coroutines.CoroutineScope
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.koin.dsl.module
import plutoproject.capability.interactive.paper.inventory.InventoryListener
import plutoproject.capability.interactive.api.GuiManager
import plutoproject.foundation.paper.coroutine.coroutineDispatcher
import plutoproject.kernel.api.Capability
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.koinGet
import plutoproject.kernel.api.loadKoinModuleDefinitions
import plutoproject.kernel.api.paper.PaperModuleContext
import plutoproject.kernel.api.exportServiceFromKoin
import java.util.logging.Logger

@Capability(id = "interactive", platform = Platform.PAPER)
class PaperInteractiveCapability : RuntimeModule {
    private var registeredListeners: List<Listener> = emptyList()

    override suspend fun onLoad(context: ModuleContext) {
        val paperContext = context as PaperModuleContext
        val interactiveScope = CoroutineScope(
            context.coroutineScope.coroutineContext + paperContext.plugin.server.coroutineDispatcher,
        )
        context.loadKoinModuleDefinitions(module {
            single<CoroutineScope> { interactiveScope }
            single<Logger> { context.logger }
            single<GuiManager> { GuiManagerImpl(get(), get()) }
            single { GuiListener(get()) }
            single { InventoryListener(get(), get()) }
        })
        context.services.exportServiceFromKoin<GuiManager>()
    }

    override suspend fun onEnable(context: ModuleContext) {
        val paperContext = context as PaperModuleContext
        val listeners: List<Listener> = listOf(
            context.koinGet<GuiListener>(),
            context.koinGet<InventoryListener>(),
        )
        listeners.forEach { paperContext.plugin.server.pluginManager.registerSuspendingEvents(it, paperContext.plugin) }
        registeredListeners = listeners
    }

    override suspend fun onDisable(context: ModuleContext) {
        registeredListeners.forEach(HandlerList::unregisterAll)
        registeredListeners = emptyList()
        context.koinGet<GuiManager>().disposeAll()
    }
}
