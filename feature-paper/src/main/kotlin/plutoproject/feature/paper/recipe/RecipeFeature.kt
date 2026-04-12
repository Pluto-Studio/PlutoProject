package plutoproject.feature.paper.recipe

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.dsl.module
import plutoproject.feature.paper.recipe.recipes.registerVanillaExtend
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.common.util.config.loadConfig
import plutoproject.framework.common.util.inject.globalKoin
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

@Feature(
    id = "recipe",
    platform = Platform.PAPER,
)
@Suppress("UNUSED")
class RecipeFeature : PaperFeature(), KoinComponent {
    private val config by inject<RecipeConfig>()
    private val featureModule = module {
        single<RecipeConfig> { loadConfig(saveConfig()) }
    }

    override fun onEnable() {
        globalKoin {
            modules(featureModule)
        }
        server.pluginManager.registerSuspendingEvents(PlayerListener, plugin)
        if (config.vanillaExtend) {
            server.registerVanillaExtend()
        }
    }
}
