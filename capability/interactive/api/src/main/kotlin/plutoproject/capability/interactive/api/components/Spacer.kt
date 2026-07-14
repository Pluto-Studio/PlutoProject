package plutoproject.capability.interactive.api.components

import androidx.compose.runtime.Composable
import plutoproject.capability.interactive.api.measuring.MeasureResult
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.capability.interactive.api.modifiers.height
import plutoproject.capability.interactive.api.layout.Layout
import plutoproject.capability.interactive.api.modifiers.width

@Composable
@Suppress("FunctionName")
fun Spacer(modifier: Modifier = Modifier) {
    Layout(
        measurePolicy = { _, constraints ->
            MeasureResult(constraints.minWidth, constraints.minHeight) {}
        },
        modifier = modifier,
    )
}

@Composable
@Suppress("FunctionName")
fun ItemSpacer() {
    Spacer(modifier = Modifier.width(1).height(1))
}

@Composable
@Suppress("FunctionName")
fun Spacer(width: Int? = null, height: Int? = null, modifier: Modifier = Modifier) {
    Spacer(
        modifier.apply {
            width?.let { width(it) } ?: this
            height?.let { height(it) } ?: this
        }
    )
}
