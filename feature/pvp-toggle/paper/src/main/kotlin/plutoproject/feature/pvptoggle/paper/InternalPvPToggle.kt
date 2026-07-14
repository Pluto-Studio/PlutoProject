package plutoproject.feature.pvptoggle.paper

import org.bukkit.entity.Player
import plutoproject.feature.pvptoggle.api.paper.PvPToggle

interface InternalPvPToggle : PvPToggle {
    suspend fun loadPlayerData(player: Player)
    fun unloadPlayerData(player: Player)
    fun clearPlayerDaya()
}
