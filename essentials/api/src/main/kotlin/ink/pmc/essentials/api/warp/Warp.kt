package ink.pmc.essentials.api.warp

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus.Internal
import java.time.Instant
import java.util.*

@Suppress("UNUSED")
interface Warp {

    val id: UUID
    val name: String
    var alias: String?
    var icon: Material?
    var category: WarpCategory?
    var type: WarpType @Internal set
    val createdAt: Instant
    var location: Location
    val isLoaded: Boolean
    val isSpawn: Boolean
    val isDefaultSpawn: Boolean

    fun teleport(player: Player, prompt: Boolean = true)

    suspend fun teleportSuspend(player: Player, prompt: Boolean = true)

    suspend fun update()

}