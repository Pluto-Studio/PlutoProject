package plutoproject.feature.paper.pvpToggle

import org.bukkit.entity.Player
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
        val damager = damager as? Player ?: return
        val victim = entity as? Player ?: return

        val damagerEnabled = PvPToggle.isPvPEnabled(damager)
        val victimEnabled = PvPToggle.isPvPEnabled(victim)

        if (!damagerEnabled || !victimEnabled) {
            isCancelled = true
            damager.showTitle(PVP_DISABLED_TITLE)
            damager.playSound(PVP_DISABLED_SOUND)
        }
    }

    suspend fun PlayerJoinEvent.e() {
        internalPvPToggle.loadPlayerData(player)
    }

    fun PlayerQuitEvent.e() {
        internalPvPToggle.unloadPlayerData(player)
    }
}
