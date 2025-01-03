package plutoproject.framework.paper.api.interactive.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import plutoproject.framework.paper.api.interactive.jetpack.Alignment
import plutoproject.framework.paper.api.interactive.jetpack.Arrangement
import plutoproject.framework.paper.api.interactive.jetpack.LayoutDirection
import plutoproject.framework.paper.api.interactive.measuring.MeasureResult
import plutoproject.framework.paper.api.interactive.measuring.Placeable
import plutoproject.framework.paper.api.interactive.measuring.RowColumnMeasurePolicy
import plutoproject.framework.paper.api.interactive.modifiers.Modifier

@Composable
fun Column(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable () -> Unit
) {
    val measurePolicy = remember(verticalArrangement, horizontalAlignment) {
        ColumnMeasurePolicy(
            verticalArrangement,
            horizontalAlignment
        )
    }
    Layout(
        measurePolicy,
        modifier = modifier,
        content = content
    )
}

private data class ColumnMeasurePolicy(
    private val verticalArrangement: Arrangement.Vertical,
    private val horizontalAlignment: Alignment.Horizontal,
) : RowColumnMeasurePolicy(
    sumHeight = true,
    arrangementSpacing = verticalArrangement.spacing
) {
    override fun placeChildren(placeables: List<Placeable>, width: Int, height: Int): MeasureResult {
        val positions = IntArray(placeables.size)
        verticalArrangement.arrange(
            totalSize = height,
            sizes = placeables.map { it.height }.toIntArray(),
            outPositions = positions
        )
        return MeasureResult(width, height) {
            var childY = 0
            placeables.forEachIndexed { index, child ->
                child.placeAt(horizontalAlignment.align(child.height, height, LayoutDirection.Ltr), positions[index])
                childY += child.height
            }
        }
    }
}
