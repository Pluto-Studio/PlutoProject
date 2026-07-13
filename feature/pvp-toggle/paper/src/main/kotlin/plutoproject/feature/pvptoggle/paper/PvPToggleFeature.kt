package plutoproject.feature.pvptoggle.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.bukkit.event.HandlerList
import org.koin.dsl.binds
import org.koin.dsl.module
import plutoproject.capability.databasepersist.api.DatabasePersist
import plutoproject.feature.menu.api.paper.MenuManager
import plutoproject.feature.pvptoggle.api.paper.PvPToggle
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "pvp_toggle",
    platform = Platform.PAPER,
    optionalFeatures = ["menu"],
    requiredCapabilities = ["database_persist"],
)
@Suppress("UNUSED")
class PvPToggleFeature : RuntimeModule {
    private val internalPvPToggle by koinInject<InternalPvPToggle>()

    override suspend fun onLoad(context: ModuleContext) {
        val databasePersist = context.services.getService<DatabasePersist>()
        context.loadKoinModuleDefinitions(module {
            single { databasePersist }
            single { PvPToggleImpl() } binds arrayOf(PvPToggle::class, InternalPvPToggle::class)
        })
        context.services.exportServiceFromKoin<PvPToggle>()
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        context.plugin.server.pluginManager.registerSuspendingEvents(PvPToggleListener, context.plugin)
        context.services.getServiceOrNull<MenuManager>()
            ?.registerButton(PvPToggleFeatureButtonDescriptor) { PvPToggleFeatureButton() }
    }

    override suspend fun onDisable(context: ModuleContext) {
        internalPvPToggle.clearPlayerDaya()
        HandlerList.unregisterAll(PvPToggleListener)
    }
}
