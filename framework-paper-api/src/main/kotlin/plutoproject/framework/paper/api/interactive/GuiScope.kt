package plutoproject.framework.paper.api.interactive

import androidx.compose.runtime.Applier
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
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
    val isBeingRefresh: MutableStateFlow<Boolean>
    val owner: Player
    val coroutineScope: CoroutineScope
    val rootNode: T
    val nodeApplier: Applier<T>

    fun setBeingRefresh(state: Boolean)

    fun dispose()
}
