package ink.pmc.interactive.api

import androidx.compose.runtime.Applier
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import org.bukkit.entity.Player

@Suppress("UNUSED")
val LocalGuiScope: ProvidableCompositionLocal<GuiScope<*>> =
    staticCompositionLocalOf { error("InteractiveScope not provided") }

@Suppress("UNUSED")
val LocalPlayer: ProvidableCompositionLocal<Player> =
    staticCompositionLocalOf { error("Player not provided") }

@Suppress("UNUSED")
interface GuiScope<T> {

    val isDisposed: Boolean
    val isPendingRefresh: MutableStateFlow<Boolean>
    val owner: Player
    val rootNode: T
    val nodeApplier: Applier<T>

    fun setPendingRefreshIfNeeded(state: Boolean)

    fun dispose()

}