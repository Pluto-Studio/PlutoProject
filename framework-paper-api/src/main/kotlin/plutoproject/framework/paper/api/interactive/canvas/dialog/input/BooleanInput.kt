package plutoproject.framework.paper.api.interactive.canvas.dialog.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.papermc.paper.registry.data.dialog.input.DialogInput
import net.kyori.adventure.text.Component

@Composable
@Suppress("UnstableApiUsage")
fun BooleanInput(
    key: String,
    label: Component = Component.empty(),
    initial: Boolean = false,
    onTrue: String = "true",
    onFalse: String = "false",
) {
    InputElement(remember(key, label, initial, onTrue, onFalse) {
        DialogInput.bool(key, label)
            .initial(initial)
            .onTrue(onTrue)
            .onFalse(onFalse)
            .build()
    })
}
