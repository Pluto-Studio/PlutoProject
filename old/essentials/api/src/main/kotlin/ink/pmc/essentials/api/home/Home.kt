package ink.pmc.essentials.api.home

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import java.time.Instant
import java.util.*

@Suppress("UNUSED")
interface Home {

    val id: UUID
    var name: String
    var icon: Material?
    val createdAt: Instant
    var location: Location
    val owner: OfflinePlayer
    var isStarred: Boolean
    val isPreferred: Boolean
    val isLoaded: Boolean

    fun teleport(player: Player, prompt: Boolean = true)

    suspend fun setPreferred(state: Boolean)

    suspend fun teleportSuspend(player: Player, prompt: Boolean = true)

    suspend fun update()

}