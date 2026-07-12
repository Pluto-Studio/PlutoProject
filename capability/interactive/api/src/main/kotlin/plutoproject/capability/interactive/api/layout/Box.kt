package plutoproject.capability.interactive.api.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import plutoproject.capability.interactive.api.jetpack.Alignment
import plutoproject.capability.interactive.api.jetpack.LayoutDirection
import plutoproject.capability.interactive.api.measuring.MeasureResult
import plutoproject.capability.interactive.api.measuring.Placeable
import plutoproject.capability.interactive.api.measuring.RowColumnMeasurePolicy
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.capability.interactive.api.util.IntSize

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
