package ink.pmc.essentials.listeners

import ink.pmc.essentials.config.EssentialsConfig
import ink.pmc.essentials.recipes.vanillaExtendRecipes
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

@Suppress("UNUSED")
object RecipeListener : Listener, KoinComponent {
    private val config by lazy { get<EssentialsConfig>().recipe }

    @EventHandler
    fun PlayerJoinEvent.e() {
        if (!config.autoUnlock) return
        player.discoverRecipes(vanillaExtendRecipes.map { it.key })
    }
}