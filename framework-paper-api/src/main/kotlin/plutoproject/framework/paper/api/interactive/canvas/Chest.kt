package plutoproject.framework.paper.api.interactive.canvas

import androidx.compose.runtime.*
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import plutoproject.framework.paper.api.interactive.*
import plutoproject.framework.paper.api.interactive.layout.Layout
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.modifiers.onSizeChanged
import plutoproject.framework.paper.api.interactive.modifiers.sizeIn
import plutoproject.framework.paper.api.interactive.node.InventoryCloseScope
import plutoproject.framework.paper.api.interactive.node.StaticMeasurePolicy
import plutoproject.framework.paper.api.interactive.util.IntCoordinates
import plutoproject.framework.paper.api.interactive.util.Size
import plutoproject.framework.paper.util.inventory.title

const val CHEST_WIDTH = 9
const val MIN_CHEST_HEIGHT = 1
const val MAX_CHEST_HEIGHT = 6

@Composable
@Suppress("UNCHECKED_CAST")
fun Chest(
    title: Component,
    modifier: Modifier = Modifier,
    onClose: (InventoryCloseScope.(player: Player) -> Unit) = {},
    content: @Composable () -> Unit,
) {
    val scope = LocalGuiScope.current as GuiInventoryScope
    var size by remember { mutableStateOf(Size()) }
    val constrainedModifier =
        Modifier.sizeIn(CHEST_WIDTH, CHEST_WIDTH, MIN_CHEST_HEIGHT, MAX_CHEST_HEIGHT).then(modifier)
            .onSizeChanged { if (size != it) size = it }

    val holder = rememberInventoryHolder(scope, onClose)

    // Create new inventory when any appropriate value changes

    // Draw nothing if empty
    if (size == Size()) {
        Layout(
            measurePolicy = StaticMeasurePolicy,
            modifier = constrainedModifier
        )
        return
    }

    val inventory: Inventory = remember(size) {
        Bukkit.createInventory(holder, CHEST_WIDTH * size.height, title).also {
            holder.activeInventory = it
        }
    }

    LaunchedEffect(title) {
        // This just sends a packet, doesn't need to be on sync thread
        inventory.viewers.forEach { it.openInventory.title(title) }
    }

    Inventory(
        inventory = inventory,
        modifier = constrainedModifier,
        gridToInventoryIndex = { (x, y) ->
            if (x !in 0 until CHEST_WIDTH || y !in 0 until size.height) null
            else x + y * CHEST_WIDTH
        },
        inventoryIndexToGrid = { index ->
            IntCoordinates(index % CHEST_WIDTH, index / CHEST_WIDTH)
        },
    ) {
        content()
    }
}
