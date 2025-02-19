package plutoproject.framework.paper.interactive.inventory

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import kotlinx.coroutines.flow.MutableStateFlow
import org.bukkit.entity.Player
import plutoproject.framework.paper.api.interactive.*
import plutoproject.framework.paper.api.interactive.click.ClickHandler
import plutoproject.framework.paper.api.interactive.click.ClickResult
import plutoproject.framework.paper.api.interactive.click.ClickScope
import plutoproject.framework.paper.api.interactive.drag.DragScope
import plutoproject.framework.paper.api.interactive.layout.Constraints
import plutoproject.framework.paper.api.interactive.node.InventoryNode
import plutoproject.framework.paper.interactive.BaseScope
import plutoproject.framework.paper.interactive.logger
import plutoproject.framework.paper.interactive.uiRenderFailed
import plutoproject.framework.paper.util.coroutine.runSync
import java.util.logging.Level

class InventoryScope(owner: Player, contents: ComposableFunction) : BaseScope<InventoryNode>(owner, contents) {
    override val rootNode: InventoryNode = InventoryNode()
    override val nodeApplier: Applier<InventoryNode> = InventoryNodeApplier(rootNode) {
        if (isDisposed) return@InventoryNodeApplier
        runCatching {
            render()
            hasFrameWaiters = false
        }.onFailure {
            renderExceptionCallback(it)
        }
    }
    override val isBeingRefresh: MutableStateFlow<Boolean> = MutableStateFlow(false)

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
                LocalPlayer provides owner
            ) {
                runCatching {
                    contents()
                }.onFailure {
                    renderExceptionCallback(it)
                }
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

    private fun renderExceptionCallback(e: Throwable) {
        owner.sendMessage(uiRenderFailed)
        logger.log(Level.SEVERE, "Inventory render failed while rendering for ${owner.name}", e)
        dispose()
    }

    override fun setBeingRefresh(state: Boolean) {
        if (state && !isBeingRefresh.value && owner.openInventory.topInventory.holder is GuiInventoryHolder) {
            isBeingRefresh.value = true
            return
        }
        if (!state && isBeingRefresh.value) {
            isBeingRefresh.value = false
            return
        }
    }

    override fun dispose() {
        if (isDisposed) return
        runSync {
            if (!owner.isOnline) return@runSync
            setBeingRefresh(true) // 防止 dispose 在事件中再次被调用造成 StackOverflowError
            owner.closeInventory()
        }
        super.dispose()
    }
}
