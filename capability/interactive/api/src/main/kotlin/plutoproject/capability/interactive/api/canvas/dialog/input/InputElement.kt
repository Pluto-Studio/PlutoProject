package plutoproject.capability.interactive.api.canvas.dialog.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import io.papermc.paper.registry.data.dialog.input.DialogInput
import plutoproject.capability.interactive.api.canvas.dialog.DialogElement
import plutoproject.capability.interactive.api.canvas.dialog.LocalDialogInputListProvider
import java.util.*

@Composable
@Suppress("UnstableApiUsage")
fun InputElement(element: DialogInput) {
    val inputList = LocalDialogInputListProvider.current
    val id = remember { UUID.randomUUID() }

    DisposableEffect(Unit) {
        onDispose {
            inputList.removeIf { it.id == id }
        }
    }

    remember(element) {
        inputList.removeIf { it.id == id }
        inputList.add(DialogElement(id, element))
    }
}
