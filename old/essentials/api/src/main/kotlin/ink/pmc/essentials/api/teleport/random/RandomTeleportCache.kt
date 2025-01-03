package ink.pmc.essentials.api.teleport.random

import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import java.util.*

data class RandomTeleportCache(
    val id: UUID,
    val world: World,
    val center: Chunk,
    val preservedChunks: Collection<Chunk>,
    val attempts: Int,
    val location: Location,
    val options: RandomTeleportOptions
)