package plutoproject.capability.interactive.api.layout

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import plutoproject.capability.interactive.api.LocalCanvas
import plutoproject.capability.interactive.api.measuring.MeasurePolicy
import plutoproject.capability.interactive.api.measuring.Renderer
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.capability.interactive.api.node.BaseInventoryNode
import plutoproject.capability.interactive.api.node.EmptyRenderer
import plutoproject.capability.interactive.api.node.InventoryNode

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
            // TODO dunno if this works
            set(canvas) { this.canvas = it }
            set(modifier) { this.modifier = it }
        },
        content = content,
    )
}
