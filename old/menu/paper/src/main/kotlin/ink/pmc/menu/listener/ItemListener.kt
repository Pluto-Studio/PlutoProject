package ink.pmc.menu.listener

import ink.pmc.framework.startScreen
import ink.pmc.framework.player.addItemOrDrop
import ink.pmc.menu.MenuConfig
import ink.pmc.menu.item.MenuItem
import ink.pmc.menu.item.MenuItemRecipe
import ink.pmc.menu.item.isMenuItem
import ink.pmc.menu.repository.UserRepository
import ink.pmc.menu.screen.MenuScreen
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("UNUSED")
object ItemListener : Listener, KoinComponent {
    private val config by inject<MenuConfig>()
    private val userRepo by inject<UserRepository>()

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
        val userModel = userRepo.findOrCreate(player.uniqueId)
        if (userModel.itemGivenServers.contains(config.serverName)) return
        player.inventory.addItemOrDrop(MenuItem)
        userRepo.saveOrUpdate(
            userModel.copy(
                itemGivenServers = buildList {
                    addAll(userModel.itemGivenServers)
                    add(config.serverName)
                }
            ))
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