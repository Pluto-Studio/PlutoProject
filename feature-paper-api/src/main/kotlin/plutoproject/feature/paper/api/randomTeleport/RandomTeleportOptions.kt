package plutoproject.feature.paper.api.randomTeleport

import org.bukkit.block.Biome
import plutoproject.framework.paper.util.world.location.Position2D

data class RandomTeleportOptions(
    val center: Position2D,
    val spawnPointAsCenter: Boolean,
    val chunkPreserveRadius: Int,
    val cacheAmount: Int,
    val startRadius: Int,
    val endRadius: Int,
    val maxHeight: Int,
    val minHeight: Int,
    val noCover: Boolean,
    val maxAttempts: Int,
    val cost: Double,
    val blacklistedBiomes: Set<Biome>,
)
