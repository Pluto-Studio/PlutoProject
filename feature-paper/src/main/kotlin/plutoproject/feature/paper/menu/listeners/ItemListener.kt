package plutoproject.feature.paper.menu.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.paper.menu.MENU_USER_MODEL_PERSIST_KEY
import plutoproject.feature.paper.menu.MenuConfig
import plutoproject.feature.paper.menu.items.MenuItem
import plutoproject.feature.paper.menu.items.MenuItemRecipe
import plutoproject.feature.paper.menu.items.isMenuItem
import plutoproject.feature.paper.menu.models.PersistUserModel
import plutoproject.feature.paper.menu.models.UserModelTypeAdapter
import plutoproject.feature.paper.menu.screens.MenuScreen
import plutoproject.framework.common.api.databasepersist.DatabasePersist
import plutoproject.framework.paper.api.interactive.startScreen
import plutoproject.framework.paper.util.inventory.addItemOrDrop

@Suppress("UNUSED")
object ItemListener : Listener, KoinComponent {
    private val config by inject<MenuConfig>()

    @EventHandler
    suspend fun PlayerJoinEvent.e() {
        if (!config.item.enabled) return
        if (config.item.registerRecipe) {
            player.discoverRecipe(MenuItemRecipe.key)
        }
        if (!config.item.giveWhenJoin) return
        if (player.inventory.contents
                .filterNotNull()
                .any { it.isMenuItem }
        ) return
        if (config.item.alwaysGive) {
            player.inventory.addItemOrDrop(MenuItem)
            return
        }
        val container = DatabasePersist.getContainer(player.uniqueId)
        val userModel = container.getOrDefault(MENU_USER_MODEL_PERSIST_KEY, UserModelTypeAdapter, PersistUserModel())
        if (userModel.itemGivenServers.contains(config.serverName)) return
        player.inventory.addItemOrDrop(MenuItem)
        val updatedModel = userModel.copy(itemGivenServers = buildList {
            addAll(userModel.itemGivenServers)
            add(config.serverName)
        })
        container.set(MENU_USER_MODEL_PERSIST_KEY, UserModelTypeAdapter, updatedModel)
        container.save()
    }

    @EventHandler
    fun PlayerInteractEvent.menu() {
        if (!config.item.enabled) return
        if (!action.isRightClick || item?.isMenuItem == false) return
        item?.let {
            isCancelled = true
            hand?.let { player.swingHand(it) }
            player.startScreen(MenuScreen())
        }
    }

    @EventHandler
    fun PlayerSwapHandItemsEvent.e() {
        if (!config.item.enabled) return
        if (!player.isSneaking) return
        isCancelled = true
        player.startScreen(MenuScreen())
    }
}
