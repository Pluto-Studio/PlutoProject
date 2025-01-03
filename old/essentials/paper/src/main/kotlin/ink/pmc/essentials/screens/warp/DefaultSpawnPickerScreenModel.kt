package ink.pmc.essentials.screens.warp

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ink.pmc.essentials.api.warp.Warp
import ink.pmc.essentials.api.warp.WarpManager
import ink.pmc.framework.interactive.layout.list.ListMenuModel
import kotlin.math.ceil

private const val PAGE_SIZE = 28

class DefaultSpawnPickerScreenModel : ListMenuModel<Warp>() {
    var isPreferredSet by mutableStateOf(false)
    var preferredSet by mutableStateOf<Warp?>(null)

    override suspend fun fetchPageContents(): List<Warp> {
        val spawns = WarpManager.listSpawns().toList()
            .sortedBy { it.createdAt }
        pageCount = ceil(spawns.size.toDouble() / PAGE_SIZE).toInt()
        return spawns.drop(page * PAGE_SIZE).take(PAGE_SIZE)
    }
}