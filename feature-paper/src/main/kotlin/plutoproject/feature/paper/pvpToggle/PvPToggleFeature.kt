package plutoproject.feature.paper.pvpToggle

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.koin.dsl.binds
import org.koin.dsl.module
import plutoproject.feature.paper.api.menu.MenuManager
import plutoproject.feature.paper.api.menu.isMenuAvailable
import plutoproject.feature.paper.api.pvpToggle.PvPToggle
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.inject.Koin
import plutoproject.framework.common.util.inject.configureKoin
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

@Feature(
    id = "pvp_toggle",
    platform = Platform.PAPER,
)
@Suppress("UNUSED")
class PvPToggleFeature : PaperFeature() {
    private val internalPvPToggle by Koin.inject<InternalPvPToggle>()
    private val featureModule = module {
        single { PvPToggleImpl() } binds arrayOf(PvPToggle::class, InternalPvPToggle::class)
    }

    override fun onEnable() {
        configureKoin {
            modules(featureModule)
        }
        server.pluginManager.registerSuspendingEvents(PvPToggleListener, plugin)
        if (isMenuAvailable) {
            MenuManager.registerButton(PvPToggleFeatureButtonDescriptor) { PvPToggleFeatureButton() }
        }
    }

    override fun onDisable() {
        internalPvPToggle.clearPlayerDaya()
    }
}
