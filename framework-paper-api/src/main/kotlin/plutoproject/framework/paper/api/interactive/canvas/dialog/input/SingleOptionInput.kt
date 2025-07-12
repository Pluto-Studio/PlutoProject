package plutoproject.framework.paper.api.interactive.canvas.dialog.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput
import net.kyori.adventure.text.Component

@Composable
@Suppress("UnstableApiUsage")
fun SingleOptionInput(
    key: String,
    label: Component = Component.empty(),
    labelVisible: Boolean = true,
    width: Int = 200,
    entries: List<SingleOptionDialogInput.OptionEntry>,
) {
    InputElement(remember(key, label, labelVisible, width, entries) {
        DialogInput.singleOption(key, label, entries)
            .labelVisible(labelVisible)
            .width(width)
            .build()
    })
}
