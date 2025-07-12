package plutoproject.framework.paper.api.interactive.canvas.dialog.body

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import io.papermc.paper.registry.data.dialog.body.DialogBody
import plutoproject.framework.paper.api.interactive.canvas.dialog.DialogElement
import plutoproject.framework.paper.api.interactive.canvas.dialog.LocalDialogBodyListProvider
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
