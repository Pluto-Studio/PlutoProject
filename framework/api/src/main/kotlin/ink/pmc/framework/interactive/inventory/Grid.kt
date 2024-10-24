package ink.pmc.framework.interactive.inventory

import androidx.compose.runtime.Composable
import ink.pmc.framework.interactive.inventory.layout.Layout
import ink.pmc.framework.interactive.inventory.layout.MeasurePolicy
import ink.pmc.framework.interactive.inventory.layout.MeasureResult

/**
 * A grid layout component that finds the largest child size, and then places children in a grid based on this size.
 *
 * Placement goes left-to-right, THEN top-to-bottom, wrapping to the next row when the width is exceeded.
 */
@Composable
fun VerticalGrid(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(
        measurePolicy = VerticalGridMeasurePolicy,
        modifier = modifier,
        content = content
    )
}

/**
 * A grid layout component that finds the largest child size, and then places children in a grid based on this size.
 *
 * Placement goes top-to-bottom, THEN left-to-right, wrapping to the next column when the height is exceeded.
 */
@Composable
fun HorizontalGrid(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(
        measurePolicy = HorizontalGridMeasurePolicy,
        modifier = modifier,
        content = content
    )
}


val VerticalGridMeasurePolicy = gridMeasurePolicy(vertical = true)
val HorizontalGridMeasurePolicy = gridMeasurePolicy(vertical = false)

fun gridMeasurePolicy(vertical: Boolean) = MeasurePolicy { measurables, constraints ->
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
