package plutoproject.capability.interactive.paper.inventory

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import plutoproject.capability.interactive.paper.BaseScope
import plutoproject.capability.interactive.paper.GuiManagerImpl
import plutoproject.capability.interactive.paper.UI_RENDER_FAILED
import plutoproject.capability.interactive.api.ComposableFunction
import plutoproject.capability.interactive.api.GuiInventoryHolder
import plutoproject.capability.interactive.api.LocalGuiScope
import plutoproject.capability.interactive.api.LocalPlayer
import plutoproject.capability.interactive.api.click.ClickHandler
import plutoproject.capability.interactive.api.click.ClickResult
import plutoproject.capability.interactive.api.click.ClickScope
import plutoproject.capability.interactive.api.drag.DragScope
import plutoproject.capability.interactive.api.layout.Constraints
import plutoproject.capability.interactive.api.LocalClickHandler
import plutoproject.capability.interactive.api.node.InventoryNode
import java.util.logging.Level
import java.util.logging.Logger

class InventoryScope(
    owner: Player,
    contents: ComposableFunction,
    manager: GuiManagerImpl,
    private val moduleScope: CoroutineScope,
    private val logger: Logger,
) : BaseScope<InventoryNode>(owner, contents, manager) {
    override val rootNode = InventoryNode()
    override val nodeApplier: Applier<InventoryNode> = InventoryNodeApplier(rootNode) {
        if (isDisposed) return@InventoryNodeApplier
        runCatching {
            render()
            hasFrameWaiters = false
        }.onFailure(::renderExceptionCallback)
    }
    override val isPendingRefresh = MutableStateFlow(false)

    private val clickHandler = object : ClickHandler {
        val rootNode = nodeApplier.current

        override suspend fun processClick(scope: ClickScope): ClickResult {
            val slot = scope.slot
            val width = rootNode.width
            return rootNode.children.fold(ClickResult()) { acc, node ->
                val w = node.width
                val x = if (w == 0) 0 else slot % width
                val y = if (w == 0) 0 else slot / width
                acc.mergeWith(rootNode.processClick(scope, x, y))
            }
        }

        override suspend fun processDrag(scope: DragScope) {
            rootNode.processDrag(scope)
        }
    }

    override val composition: Composition = Composition(nodeApplier, recomposer).apply {
        setContent {
            CompositionLocalProvider(
                LocalGuiScope provides this@InventoryScope,
                LocalClickHandler provides clickHandler,
                LocalPlayer provides owner,
            ) {
                contents()
            }
        }
    }

    private fun render() {
        nodeApplier.current.apply {
            measure(Constraints())
            render()
            owner.updateInventory()
        }
    }

    private fun renderExceptionCallback(exception: Throwable) {
        owner.sendMessage(UI_RENDER_FAILED)
        logger.log(
            Level.SEVERE,
            "Inventory render failed while rendering for ${owner.name}",
            exception,
        )
        dispose()
    }

    override fun setPendingRefreshIfNeeded(state: Boolean) {
        if (state && !isPendingRefresh.value && owner.openInventory.topInventory.holder is GuiInventoryHolder) {
            isPendingRefresh.value = true
            return
        }
        if (!state && isPendingRefresh.value) isPendingRefresh.value = false
    }

    override fun dispose() {
        if (isDisposed) return
        moduleScope.launch {
            if (!owner.isOnline) return@launch
            setPendingRefreshIfNeeded(true)
            owner.closeInventory()
        }
        super.dispose()
    }
}
