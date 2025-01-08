package plutoproject.feature.paper.dynamicScheduler

import org.bukkit.entity.SpawnCategory

data class SpawnCurve(
    val category: SpawnCategory,
    val curve: Double2IntCurve
)
