package plutoproject.capability.interactive.api.layout

import androidx.compose.runtime.Composable
import plutoproject.capability.interactive.api.measuring.MeasurePolicy
import plutoproject.capability.interactive.api.measuring.MeasureResult
import plutoproject.capability.interactive.api.modifiers.Modifier

@Suppress("FunctionName")
@Composable
fun VerticalGrid(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(
        measurePolicy = VerticalGridMeasurePolicy,
        modifier = modifier,
        content = content
    )
}

@Suppress("FunctionName")
@Composable
fun HorizontalGrid(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(
        measurePolicy = HorizontalGridMeasurePolicy,
        modifier = modifier,
        content = content
    )
}

val VerticalGridMeasurePolicy = GridMeasurePolicy(vertical = true)
val HorizontalGridMeasurePolicy = GridMeasurePolicy(vertical = false)

fun GridMeasurePolicy(vertical: Boolean) = MeasurePolicy { measurables, constraints ->
    val noMinConstraints = constraints.copy(minWidth = 0, minHeight = 0)
    val placeables = measurables.map { it.measure(noMinConstraints) }
    val cellWidth = placeables.maxOfOrNull { it.width } ?: 0
    val cellHeight = placeables.maxOfOrNull { it.height } ?: 0
    if (cellWidth == 0 || cellHeight == 0) return@MeasurePolicy MeasureResult(
        constraints.minWidth,
        constraints.minHeight
    ) {}

    // Get width and height divisible by cellWidth, cellHeight
    val itemsPerLine =
        if (vertical) constraints.maxWidth / cellWidth
        else constraints.maxHeight / cellHeight

    val (width, height) = if (vertical) {
        val w = itemsPerLine * cellWidth
        val h = ((placeables.size / itemsPerLine) + 1) * cellHeight
        w to h
    } else {
        val w = ((placeables.size / itemsPerLine) + 1) * cellWidth
        val h = itemsPerLine * cellHeight
        w to h
    }

    MeasureResult(width, height) {
        var placeAtX = 0
        var placeAtY = 0
        for (child in placeables) {
            child.placeAt(placeAtX, placeAtY)

            if (vertical) {
                placeAtX += cellWidth
                if (placeAtX >= width) {
                    placeAtX = 0
                    placeAtY += cellHeight
                    if (placeAtY + cellHeight > height) break
                }
            } else {
                placeAtY += cellHeight
                if (placeAtY >= height) {
                    placeAtY = 0
                    placeAtX += cellWidth
                    if (placeAtX + cellWidth > width) break
                }
            }
        }
    }
}
