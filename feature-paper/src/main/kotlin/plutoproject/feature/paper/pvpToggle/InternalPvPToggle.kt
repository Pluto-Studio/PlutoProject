package plutoproject.feature.paper.pvpToggle

import org.bukkit.entity.Player
import plutoproject.feature.paper.api.pvpToggle.PvPToggle

interface InternalPvPToggle : PvPToggle {
    suspend fun loadPlayerData(player: Player)
    fun unloadPlayerData(player: Player)
    fun clearPlayerDaya()
}
