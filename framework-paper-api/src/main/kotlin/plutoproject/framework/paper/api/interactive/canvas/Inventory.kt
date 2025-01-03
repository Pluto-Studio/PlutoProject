package plutoproject.framework.paper.api.interactive.canvas

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.inventory.Inventory
import plutoproject.framework.common.util.time.ticks
import plutoproject.framework.paper.api.interactive.*
import plutoproject.framework.paper.api.interactive.click.ClickScope
import plutoproject.framework.paper.api.interactive.drag.DragScope
import plutoproject.framework.paper.api.interactive.layout.Layout
import plutoproject.framework.paper.api.interactive.measuring.Renderer
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.node.BaseInventoryNode
import plutoproject.framework.paper.api.interactive.node.InventoryCloseScope
import plutoproject.framework.paper.api.interactive.node.StaticMeasurePolicy
import plutoproject.framework.paper.api.interactive.util.IntCoordinates
import plutoproject.framework.paper.util.coroutine.runSync
import plutoproject.framework.paper.util.coroutine.withSync

val LocalInventory: ProvidableCompositionLocal<Inventory> =
    compositionLocalOf { error("No local inventory defined") }

@Composable
@Suppress("UNCHECKED_CAST")
fun Inventory(
    inventory: Inventory,
    modifier: Modifier = Modifier,
    gridToInventoryIndex: (IntCoordinates) -> Int?,
    inventoryIndexToGrid: (Int) -> IntCoordinates,
    content: @Composable () -> Unit,
) {
    val player = LocalPlayer.current
    val scope = LocalGuiScope.current
    val canvas = remember { MapCanvas() }

    LaunchedEffect(player) {
        withSync {
            scope.setPendingRefreshIfNeeded(true)
            player.openInventory(inventory)
        }
    }

    val renderer = object : Renderer {
        override fun Canvas.render(node: BaseInventoryNode) {
            canvas.startRender()
        }

        override fun Canvas.renderAfterChildren(node: BaseInventoryNode) {
            val items = canvas.getCoordinates()
            repeat(inventory.size) { index ->
                val coords = inventoryIndexToGrid(index)
                if (items[coords] == null) inventory.setItem(index, null)
            }
            for ((coords, item) in items) {
                val index = gridToInventoryIndex(coords) ?: continue
                if (index !in 0..<inventory.size) continue
                val invItem = inventory.getItem(index)
                if (invItem != item) inventory.setItem(index, item)
            }
        }
    }

    CompositionLocalProvider(
        LocalCanvas provides canvas,
        LocalInventory provides inventory
    ) {
        Layout(
            measurePolicy = StaticMeasurePolicy,
            renderer = renderer,
            modifier = modifier,
            content = content,
        )
    }
}

@Composable
inline fun rememberInventoryHolder(
    session: GuiInventoryScope,
    crossinline onClose: InventoryCloseScope.(Player) -> Unit = {},
): GuiInventoryHolder {
    val clickHandler = LocalClickHandler.current
    return remember(clickHandler) {
        object : GuiInventoryHolder(session) {
            override suspend fun processClick(scope: ClickScope, event: Cancellable) {
                val clickResult = clickHandler.processClick(scope)
            }

            override suspend fun processDrag(scope: DragScope) {
                clickHandler.processDrag(scope)
            }

            override fun onClose(player: Player) {
                val scope = object : InventoryCloseScope {
                    override fun reopen() {
                        // TODO don't think this reference updates properly in the remember block
                        if (player.openInventory.topInventory != inventory) {
                            player.openInventory(inventory)
                        }
                    }
                }
                runSync {
                    delay(1.ticks)
                    onClose.invoke(scope, player)
                }
            }
        }
    }
}
