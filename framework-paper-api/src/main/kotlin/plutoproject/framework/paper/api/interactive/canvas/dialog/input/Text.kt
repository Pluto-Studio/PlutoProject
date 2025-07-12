package plutoproject.framework.paper.api.interactive.canvas.dialog.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.TextDialogInput
import net.kyori.adventure.text.Component

@Composable
@Suppress("UnstableApiUsage")
fun TextInput(
    key: String,
    label: Component = Component.empty(),
    labelVisible: Boolean = true,
    width: Int = 200,
    initial: String = "",
    maxLength: Int = 32,
    multiline: TextDialogInput.MultilineOptions? = null,
) {
    InputElement(remember(key, label, labelVisible, width, initial, maxLength, multiline) {
        DialogInput.text(key, label)
            .labelVisible(labelVisible)
            .width(width)
            .initial(initial)
            .maxLength(maxLength)
            .multiline(multiline)
            .build()
    })
}
