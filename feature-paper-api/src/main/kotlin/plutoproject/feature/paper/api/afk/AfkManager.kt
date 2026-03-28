package plutoproject.feature.paper.api.afk

import org.bukkit.entity.Player
import plutoproject.framework.common.util.inject.globalKoin
import kotlin.time.Duration

interface AfkManager {
    companion object : AfkManager by globalKoin.get()

    val afkSet: Set<Player>
    val idleDuration: Duration

    fun isAfk(player: Player): Boolean

    fun set(player: Player, state: Boolean, manually: Boolean = false)

    fun toggle(player: Player, manually: Boolean = false)
}
