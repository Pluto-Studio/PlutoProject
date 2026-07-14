package plutoproject.feature.recipe.paper

import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.PropertySource
import com.sksamuel.hoplite.hocon.HoconParser
import org.bukkit.event.HandlerList
import org.koin.dsl.module
import plutoproject.feature.recipe.paper.recipes.registerVanillaExtend
import plutoproject.kernel.api.*
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "recipe",
    platform = Platform.PAPER,
)
@Suppress("UNUSED")
@OptIn(ExperimentalHoplite::class)
class RecipeFeature : RuntimeModule {
    private val config by koinInject<RecipeConfig>()

    override suspend fun onLoad(context: ModuleContext) {
        val configFile = context.saveResource("config.conf")
        val loadedConfig = ConfigLoaderBuilder.empty()
            .withClassLoader(RecipeFeature::class.java.classLoader)
            .withExplicitSealedTypes()
            .addDefaults()
            .addParser("conf", HoconParser())
            .addPropertySource(PropertySource.file(configFile.toFile()))
            .build()
            .loadConfigOrThrow<RecipeConfig>()
        context.loadKoinModuleDefinitions(module { single { loadedConfig } })
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        context.plugin.server.pluginManager.registerSuspendingEvents(PlayerListener, context.plugin)
        if (config.vanillaExtend) {
            context.plugin.server.registerVanillaExtend()
        }
    }

    override suspend fun onDisable(context: ModuleContext) {
        HandlerList.unregisterAll(PlayerListener)
    }
}
