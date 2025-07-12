package plutoproject.framework.paper.api.interactive.canvas.dialog.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.papermc.paper.registry.data.dialog.input.DialogInput
import net.kyori.adventure.text.Component

@Composable
@Suppress("UnstableApiUsage")
fun NumberRangeInput(
    key: String,
    label: Component = Component.empty(),
    start: Float = 0f,
    end: Float = 100f,
    width: Int = 200,
    labelFormat: String = "options.generic_value",
    initial: Float? = null,
    step: Float? = null,
) {
    InputElement(remember(key, label, start, end, width, labelFormat, initial, step) {
        DialogInput.numberRange(key, label, start, end)
            .width(width)
            .labelFormat(labelFormat)
            .initial(initial)
            .step(step)
            .build()
    })
}
