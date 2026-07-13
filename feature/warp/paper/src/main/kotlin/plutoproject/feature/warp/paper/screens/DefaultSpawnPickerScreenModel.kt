package plutoproject.feature.warp.paper.screens

import plutoproject.feature.warp.paper.warpManager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import plutoproject.feature.warp.api.paper.Warp
import plutoproject.feature.warp.api.paper.WarpManager
import plutoproject.capability.interactive.api.layout.list.ListMenuModel
import kotlin.math.ceil

private const val PAGE_SIZE = 28

class DefaultSpawnPickerScreenModel : ListMenuModel<Warp>() {
    var isPreferredSet by mutableStateOf(false)
    var preferredSet by mutableStateOf<Warp?>(null)

    override suspend fun fetchPageContents(): List<Warp> {
        val spawns = warpManager.listSpawns().toList()
            .sortedBy { it.createdAt }
        pageCount = ceil(spawns.size.toDouble() / PAGE_SIZE).toInt()
        return spawns.drop(page * PAGE_SIZE).take(PAGE_SIZE)
    }
}
