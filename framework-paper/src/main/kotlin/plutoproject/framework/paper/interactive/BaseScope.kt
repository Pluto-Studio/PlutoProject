package plutoproject.framework.paper.interactive

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.*
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.framework.paper.api.interactive.ComposableFunction
import plutoproject.framework.paper.api.interactive.GuiManager
import plutoproject.framework.paper.api.interactive.GuiScope
import kotlin.coroutines.CoroutineContext

@Suppress("UNUSED")
abstract class BaseScope<T>(
    override val owner: Player,
    private val contents: ComposableFunction
) : GuiScope<T>, KoinComponent {
    var hasFrameWaiters: Boolean = false
    private val manager by inject<GuiManager>()
    private var hasSnapshotNotifications: Boolean = false
    private val frameClock: BroadcastFrameClock = BroadcastFrameClock { hasFrameWaiters = true }
    private val coroutineContext: CoroutineContext = Dispatchers.Default + frameClock
    final override val coroutineScope = CoroutineScope(coroutineContext)
    private val observerHandle: ObserverHandle = Snapshot.Companion.registerGlobalWriteObserver {
        if (!hasSnapshotNotifications) {
            hasSnapshotNotifications = true
            coroutineScope.launch {
                hasSnapshotNotifications = false
                Snapshot.Companion.sendApplyNotifications()
            }
        }
    }
    protected val recomposer: Recomposer = Recomposer(coroutineContext)

    override var isDisposed: Boolean = false
    abstract val composition: Composition

    init {
        coroutineScope.launch {
            recomposer.runRecomposeAndApplyChanges()
        }

        coroutineScope.launch {
            while (!isDisposed) {
                frameClock.sendFrame(System.nanoTime())
                delay(10)
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