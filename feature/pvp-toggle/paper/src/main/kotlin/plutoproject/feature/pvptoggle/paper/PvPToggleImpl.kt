package plutoproject.feature.pvptoggle.paper

import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import plutoproject.capability.databasepersist.api.DatabasePersist
import plutoproject.capability.databasepersist.api.adapters.BooleanTypeAdapter
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.koinInject
import java.util.*

class PvPToggleImpl : InternalPvPToggle {
    private val databasePersist by koinInject<DatabasePersist>()
    private val pvpEnabledCache = mutableMapOf<UUID, Boolean>()

    override fun isPvPEnabled(player: Player): Boolean {
        return pvpEnabledCache[player.uniqueId] ?: false
    }

    override fun setPvPEnabled(player: Player, enabled: Boolean) {
        pvpEnabledCache[player.uniqueId] = enabled
        currentModuleContext().coroutineScope.launch {
            val container = databasePersist.getContainer(player.uniqueId)
            container.set(PVP_TOGGLE_PERSIST_KEY, BooleanTypeAdapter, enabled)
            container.save()
        }
    }

    override suspend fun loadPlayerData(player: Player) {
        val container = databasePersist.getContainer(player.uniqueId)
        val enabled = container.getOrElse(PVP_TOGGLE_PERSIST_KEY, BooleanTypeAdapter) { false }
        pvpEnabledCache[player.uniqueId] = enabled
    }

    override fun unloadPlayerData(player: Player) {
        pvpEnabledCache.remove(player.uniqueId)
    }

    override fun clearPlayerDaya() {
        pvpEnabledCache.clear()
    }
}
