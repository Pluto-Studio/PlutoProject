package ink.pmc.essentials.api.afk

import ink.pmc.framework.inject.inlinedGet
import org.bukkit.entity.Player
import kotlin.time.Duration

interface AfkManager {
    companion object : AfkManager by inlinedGet()

    val afkSet: Set<Player>
    val idleDuration: Duration

    fun isAfk(player: Player): Boolean

    fun set(player: Player, state: Boolean, manually: Boolean = false)

    fun toggle(player: Player, manually: Boolean = false)
}