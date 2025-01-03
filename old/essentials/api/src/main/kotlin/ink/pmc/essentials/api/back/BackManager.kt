package ink.pmc.essentials.api.back

import ink.pmc.framework.inject.inlinedGet
import org.bukkit.Location
import org.bukkit.entity.Player

@Suppress("UNUSED")
interface BackManager {
    companion object : BackManager by inlinedGet()

    suspend fun has(player: Player): Boolean

    suspend fun get(player: Player): Location?

    fun back(player: Player)

    suspend fun backSuspend(player: Player)

    suspend fun set(player: Player, location: Location)

    suspend fun remove(player: Player)
}