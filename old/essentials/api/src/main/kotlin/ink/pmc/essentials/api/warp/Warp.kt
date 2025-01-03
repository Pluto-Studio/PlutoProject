package ink.pmc.essentials.api.warp

import kotlinx.coroutines.Deferred
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus.Internal
import java.time.Instant
import java.util.*

@Suppress("UNUSED")
interface Warp {

    val id: UUID
    val name: String
    var alias: String?
    var founderId: UUID?
    val founder: Deferred<OfflinePlayer>?
    var icon: Material?
    var category: WarpCategory?
    var description: Component?
    var type: WarpType @Internal set
    val createdAt: Instant
    var location: Location
    val isSpawn: Boolean
    val isDefaultSpawn: Boolean

    fun teleport(player: Player, prompt: Boolean = true)

    suspend fun teleportSuspend(player: Player, prompt: Boolean = true)

    suspend fun update()

}