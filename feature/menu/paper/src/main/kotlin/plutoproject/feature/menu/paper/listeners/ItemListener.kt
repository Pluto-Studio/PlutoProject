package plutoproject.feature.menu.paper.listeners

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import plutoproject.kernel.api.koinInject
import plutoproject.feature.menu.paper.MENU_USER_MODEL_PERSIST_KEY
import plutoproject.feature.menu.paper.MenuConfig
import plutoproject.feature.menu.paper.items.MenuItem
import plutoproject.feature.menu.paper.items.MenuItemRecipe
import plutoproject.feature.menu.paper.items.isMenuItem
import plutoproject.feature.menu.paper.models.PersistUserModel
import plutoproject.feature.menu.paper.models.UserModelTypeAdapter
import plutoproject.feature.menu.paper.screens.MenuScreen
import plutoproject.capability.databasepersist.api.DatabasePersist
import plutoproject.feature.menu.paper.startScreen
import plutoproject.foundation.paper.inventory.addItemOrDrop

@Suppress("UNUSED")
object ItemListener : Listener {
    private val config by koinInject<MenuConfig>()

    @EventHandler
    suspend fun PlayerJoinEvent.e() {
        if (!config.item.enabled) return
        if (config.item.registerRecipe) {
            player.discoverRecipe(MenuItemRecipe.key)
        }
        if (!config.item.giveWhenJoin) return
        if (!player.hasPermission("plutoproject.menu.receive_menu_item")) return
        if (player.inventory.contents
                .filterNotNull()
                .any { it.isMenuItem }
        ) return
        if (config.item.alwaysGive) {
            player.inventory.addItemOrDrop(MenuItem)
            return
        }
        val container = plutoproject.kernel.api.koinGet<DatabasePersist>().getContainer(player.uniqueId)
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
        if (!player.hasPermission("plutoproject.menu.interact.use_menu_item")) return
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
        if (!player.hasPermission("plutoproject.menu.interact.shortcut_to_open")) return
        isCancelled = true
        player.startScreen(MenuScreen())
    }
}
