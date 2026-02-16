package plutoproject.feature.paper.pvpToggle

import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import plutoproject.feature.paper.api.pvpToggle.PvPToggle
import plutoproject.framework.common.util.inject.Koin

object PvPToggleListener : Listener {
    private val internalPvPToggle: InternalPvPToggle by Koin.inject()

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun EntityDamageByEntityEvent.e() {
        val damager = when (val rawDamager = damager) {
            is Player -> rawDamager
            is Projectile -> rawDamager.shooter as? Player ?: return
            else -> return
        }
        val victim = entity as? Player ?: return

        if (damager.hasPermission(PVP_TOGGLE_BYPASS_PERMISSION)) {
            return
        }

        val damagerEnabled = PvPToggle.isPvPEnabled(damager)
        val victimEnabled = PvPToggle.isPvPEnabled(victim)

        if (!damagerEnabled) {
            isCancelled = true
            damager.showTitle(PVP_DISABLED_TITLE_DAMAGER)
            return
        }

        if (!victimEnabled) {
            isCancelled = true
            damager.showTitle(PVP_DISABLED_TITLE_VICTIM)
            return
        }
    }

    @EventHandler
    suspend fun PlayerJoinEvent.e() {
        internalPvPToggle.loadPlayerData(player)
    }

    @EventHandler
    fun PlayerQuitEvent.e() {
        internalPvPToggle.unloadPlayerData(player)
    }
}
