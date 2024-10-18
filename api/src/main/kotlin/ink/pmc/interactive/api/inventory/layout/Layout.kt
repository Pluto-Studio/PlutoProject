package ink.pmc.interactive.api.inventory.layout

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import ink.pmc.interactive.api.inventory.LocalCanvas
import ink.pmc.interactive.api.inventory.modifiers.Modifier
import ink.pmc.interactive.api.inventory.nodes.BaseInventoryNode

/**
 * The main component for layout, it measures and positions zero or more children.
 */
@Composable
inline fun Layout(
    measurePolicy: MeasurePolicy,
    renderer: Renderer = EmptyRenderer,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    val canvas = LocalCanvas.current
    ComposeNode<BaseInventoryNode, Applier<InventoryNode>>(
        factory = BaseInventoryNode.Constructor,
        update = {
            set(measurePolicy) { this.measurePolicy = it }
            set(renderer) { this.renderer = it }
            //TODO dunno if this works
            set(canvas) { this.canvas = it }
            set(modifier) { this.modifier = it }
        },
        content = content,
    )
}
