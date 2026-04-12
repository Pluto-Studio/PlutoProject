package plutoproject.feature.paper.api.back

import org.bukkit.Location
import org.bukkit.entity.Player
import plutoproject.framework.common.util.inject.globalKoin

@Suppress("UNUSED")
interface BackManager {
    companion object : BackManager by globalKoin.get()

    suspend fun has(player: Player): Boolean

    suspend fun get(player: Player): Location?

    fun back(player: Player)

    suspend fun backSuspend(player: Player)

    suspend fun set(player: Player, location: Location)

    suspend fun remove(player: Player)
}
