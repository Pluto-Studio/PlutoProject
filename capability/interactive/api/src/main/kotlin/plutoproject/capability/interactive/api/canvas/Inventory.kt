package plutoproject.capability.interactive.api.canvas

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.inventory.Inventory
import plutoproject.capability.interactive.api.*
import plutoproject.capability.interactive.api.click.ClickScope
import plutoproject.capability.interactive.api.drag.DragScope
import plutoproject.capability.interactive.api.layout.Layout
import plutoproject.capability.interactive.api.measuring.Renderer
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.capability.interactive.api.node.BaseInventoryNode
import plutoproject.capability.interactive.api.node.InventoryCloseScope
import plutoproject.capability.interactive.api.node.StaticMeasurePolicy
import plutoproject.capability.interactive.api.util.IntCoordinates

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

    LaunchedEffect(player, inventory) {
        scope.coroutineScope.launch {
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
                val closeScope = object : InventoryCloseScope {
                    override fun reopen() {
                        // TODO don't think this reference updates properly in the remember block
                        if (player.openInventory.topInventory != inventory) {
                            player.openInventory(inventory)
                        }
                    }
                }
                session.coroutineScope.launch {
                    delay(50)
                    onClose.invoke(closeScope, player)
                }
            }
        }
    }
}
