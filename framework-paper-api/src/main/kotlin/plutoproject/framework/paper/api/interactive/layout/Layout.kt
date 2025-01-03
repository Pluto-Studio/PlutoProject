package plutoproject.framework.paper.api.interactive.layout

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import plutoproject.framework.paper.api.interactive.LocalCanvas
import plutoproject.framework.paper.api.interactive.measuring.MeasurePolicy
import plutoproject.framework.paper.api.interactive.measuring.Renderer
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.node.BaseInventoryNode
import plutoproject.framework.paper.api.interactive.node.EmptyRenderer
import plutoproject.framework.paper.api.interactive.node.InventoryNode

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
