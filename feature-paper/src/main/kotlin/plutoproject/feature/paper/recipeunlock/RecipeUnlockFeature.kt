package plutoproject.feature.paper.recipeunlock

import org.bukkit.Keyed
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.annotation.Feature
import plutoproject.framework.paper.api.feature.PaperFeature
import plutoproject.framework.paper.util.plugin
import plutoproject.framework.paper.util.server

@Feature(
    id = "recipe_unlock",
    platform = Platform.PAPER,
)
@Suppress("UNUSED")
class RecipeUnlockFeature : PaperFeature(), Listener {
    private val allRecipeKeys = buildList {
        server.recipeIterator().forEach {
            if (it is Keyed) {
                add(it.key)
            }
        }
    }

    override fun onEnable() {
        server.pluginManager.registerEvents(this, plugin)
    }

    @EventHandler
    fun PlayerJoinEvent.onPlayerJoin() {
        player.discoverRecipes(allRecipeKeys)
    }
}
