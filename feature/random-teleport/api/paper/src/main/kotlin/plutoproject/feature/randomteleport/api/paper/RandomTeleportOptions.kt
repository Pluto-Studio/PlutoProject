package plutoproject.feature.randomteleport.api.paper

import org.bukkit.block.Biome
import plutoproject.feature.randomteleport.api.paper.Position2D

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
