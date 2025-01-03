package plutoproject.framework.paper.api.interactive.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import plutoproject.framework.paper.api.interactive.jetpack.Alignment
import plutoproject.framework.paper.api.interactive.jetpack.LayoutDirection
import plutoproject.framework.paper.api.interactive.measuring.MeasureResult
import plutoproject.framework.paper.api.interactive.measuring.Placeable
import plutoproject.framework.paper.api.interactive.measuring.RowColumnMeasurePolicy
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.util.IntSize

@Composable
@Suppress("FunctionName")
fun Box(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit
) {
    val measurePolicy = remember(contentAlignment) { BoxMeasurePolicy(contentAlignment) }
    Layout(
        measurePolicy,
        modifier = modifier,
        content = content
    )
}

internal data class BoxMeasurePolicy(
    private val alignment: Alignment,
) : RowColumnMeasurePolicy() {
    override fun placeChildren(placeables: List<Placeable>, width: Int, height: Int): MeasureResult {
        return MeasureResult(width, height) {
            for (child in placeables) {
                child.placeAt(alignment.align(child.size, IntSize(width, height), LayoutDirection.Ltr))
            }
        }
    }
}
