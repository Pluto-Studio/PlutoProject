package plutoproject.feature.recipe.paper

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import plutoproject.feature.recipe.paper.recipes.vanillaExtendRecipes

@Suppress("UNUSED")
object PlayerListener : Listener {
    private val config by plutoproject.kernel.api.koinInject<RecipeConfig>()

    @EventHandler
    fun PlayerJoinEvent.e() {
        if (!config.autoUnlock) return
        player.discoverRecipes(vanillaExtendRecipes.map { it.key })
    }
}
