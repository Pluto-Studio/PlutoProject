package plutoproject.capability.interactive.api.canvas.dialog.body

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import io.papermc.paper.registry.data.dialog.body.DialogBody
import plutoproject.capability.interactive.api.canvas.dialog.DialogElement
import plutoproject.capability.interactive.api.canvas.dialog.LocalDialogBodyListProvider
import java.util.*

@Composable
@Suppress("UnstableApiUsage")
fun BodyElement(element: DialogBody) {
    val bodyList = LocalDialogBodyListProvider.current
    val id = remember { UUID.randomUUID() }

    DisposableEffect(Unit) {
        onDispose {
            bodyList.removeIf { it.id == id }
        }
    }

    remember(element) {
        bodyList.removeIf { it.id == id }
        bodyList.add(DialogElement(id, element))
    }
}
