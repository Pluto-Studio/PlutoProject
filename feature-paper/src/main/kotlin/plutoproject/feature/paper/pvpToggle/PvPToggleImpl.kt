package plutoproject.feature.paper.pvpToggle

import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import plutoproject.framework.common.api.databasepersist.DatabasePersist
import plutoproject.framework.common.api.databasepersist.adapters.BooleanTypeAdapter
import plutoproject.framework.common.util.coroutine.PluginScope
import java.util.*

class PvPToggleImpl : InternalPvPToggle {
    private val pvpEnabledCache = mutableMapOf<UUID, Boolean>()

    override fun isPvPEnabled(player: Player): Boolean {
        return pvpEnabledCache[player.uniqueId] ?: false
    }

    override fun setPvPEnabled(player: Player, enabled: Boolean) {
        pvpEnabledCache[player.uniqueId] = enabled
        PluginScope.launch {
            val container = DatabasePersist.getContainer(player.uniqueId)
            container.set(PVP_TOGGLE_PERSIST_KEY, BooleanTypeAdapter, enabled)
            container.save()
        }
    }

    override suspend fun loadPlayerData(player: Player) {
        val container = DatabasePersist.getContainer(player.uniqueId)
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
