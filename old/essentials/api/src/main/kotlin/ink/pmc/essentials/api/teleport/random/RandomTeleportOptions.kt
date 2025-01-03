package ink.pmc.essentials.api.teleport.random

import ink.pmc.framework.world.Vec2
import org.bukkit.block.Biome

data class RandomTeleportOptions(
    val center: Vec2,
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