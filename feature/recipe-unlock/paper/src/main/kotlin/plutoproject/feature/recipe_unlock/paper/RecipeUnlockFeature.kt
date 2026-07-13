package plutoproject.feature.recipe_unlock.paper

import org.bukkit.Keyed
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import plutoproject.kernel.api.Feature
import plutoproject.kernel.api.ModuleContext
import plutoproject.kernel.api.Platform
import plutoproject.kernel.api.RuntimeModule
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.paper.PaperModuleContext

@Feature(
    id = "recipe_unlock",
    platform = Platform.PAPER,
)
@Suppress("UNUSED")
class RecipeUnlockFeature : RuntimeModule, Listener {
    private val allRecipeKeys = buildList {
        val server = (currentModuleContext() as PaperModuleContext).plugin.server
        server.recipeIterator().forEach {
            if (it is Keyed) add(it.key)
        }
    }

    override suspend fun onEnable(context: ModuleContext) {
        context as PaperModuleContext
        context.plugin.server.pluginManager.registerEvents(this, context.plugin)
    }

    override suspend fun onDisable(context: ModuleContext) {
        HandlerList.unregisterAll(this)
    }

    @EventHandler
    fun PlayerJoinEvent.onPlayerJoin() {
        player.discoverRecipes(allRecipeKeys)
    }
}
