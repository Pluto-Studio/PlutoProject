package plutoproject.framework.paper.api.interactive.components

import androidx.compose.runtime.Composable
import plutoproject.framework.paper.api.interactive.measuring.MeasureResult
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.modifiers.height
import plutoproject.framework.paper.api.interactive.layout.Layout
import plutoproject.framework.paper.api.interactive.modifiers.width

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
