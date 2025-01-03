package ink.pmc.misc.listeners

import ink.pmc.misc.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.*
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent

@Suppress("UNUSED")
object SitListener : Listener {

    @EventHandler
    fun PlayerQuitEvent.e() {
        handlePlayerQuit(this)
    }

    @EventHandler
    fun EntityDismountEvent.e() {
        handleSitDelay(this)
    }

    @EventHandler
    fun EntityDamageEvent.e() {
        tryToStand(this, false)
        handleArmorStandAction(this)
    }

    @EventHandler
    fun EntityDeathEvent.e() {
        tryToStand(this, false)
        handleArmorStandAction(this)
    }

    @EventHandler
    fun EntityTeleportEvent.e() {
        tryToStand(this, false)
        handleArmorStandAction(this)
    }

    @EventHandler
    fun EntityMountEvent.e() {
        // tryToStand(this, false) 暂时移除，可能有问题
        handleArmorStandAction(this)
    }

    @EventHandler
    fun BlockBreakEvent.e() {
        handleSitLocationBroke(this)
    }

    @EventHandler
    fun BlockBurnEvent.e() {
        handleSitLocationBroke(this)
    }

    @EventHandler
    fun BlockExplodeEvent.e() {
        handleSitLocationBroke(this)
    }

    @EventHandler
    fun BlockFadeEvent.e() {
        handleSitLocationBroke(this)
    }

    @EventHandler
    fun BlockIgniteEvent.e() {
        handleSitLocationBroke(this)
    }

    @EventHandler
    fun EntityExplodeEvent.e() {
        handleSitLocationBroke(this)
    }

    @EventHandler
    fun PlayerInteractEvent.e() {
        handleSitClick(this)
    }

    @EventHandler
    fun BlockPistonExtendEvent.e() {
        handleSitLocationBroke(this)
    }

    @EventHandler
    fun BlockPistonRetractEvent.e() {
        handleSitLocationBroke(this)
    }

    @EventHandler
    fun EntitySpawnEvent.e() {
        handleSitLocationBroke(this)
    }

    @EventHandler
    fun LeavesDecayEvent.e() {
        handleSitLocationBroke(this)
    }

}