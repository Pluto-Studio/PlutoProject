package plutoproject.capability.interactive.paper

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import plutoproject.capability.interactive.api.ComposableFunction
import plutoproject.capability.interactive.api.GuiManager
import plutoproject.capability.interactive.api.GuiScope
import kotlin.coroutines.CoroutineContext

@Suppress("UNUSED")
abstract class BaseScope<T>(
    override val owner: Player,
    private val contents: ComposableFunction,
    private val manager: GuiManager,
) : GuiScope<T> {
    var hasFrameWaiters: Boolean = false
    private var hasSnapshotNotifications: Boolean = false
    private val frameClock = BroadcastFrameClock { hasFrameWaiters = true }
    private val coroutineContext: CoroutineContext = Dispatchers.Default + frameClock
    final override val coroutineScope = CoroutineScope(coroutineContext)
    private val observerHandle: ObserverHandle = Snapshot.registerGlobalWriteObserver {
        if (!hasSnapshotNotifications) {
            hasSnapshotNotifications = true
            coroutineScope.launch {
                hasSnapshotNotifications = false
                Snapshot.sendApplyNotifications()
            }
        }
    }
    protected val recomposer = Recomposer(coroutineContext)

    override var isDisposed: Boolean = false
    abstract val composition: Composition

    init {
        coroutineScope.launch { recomposer.runRecomposeAndApplyChanges() }
        coroutineScope.launch {
            while (!isDisposed) {
                if (hasFrameWaiters) frameClock.sendFrame(System.nanoTime())
                delay(20)
            }
        }
    }

    override fun dispose() {
        if (isDisposed) return
        isDisposed = true
        composition.dispose()
        frameClock.cancel()
        recomposer.cancel()
        observerHandle.dispose()
        coroutineScope.cancel()
        manager.removeScope(this)
    }
}
