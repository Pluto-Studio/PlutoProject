package plutoproject.framework.paper.api.interactive.canvas.dialog.body

import androidx.compose.runtime.Composable
import io.papermc.paper.registry.data.dialog.body.DialogBody
import net.kyori.adventure.text.Component
import plutoproject.framework.paper.api.interactive.canvas.dialog.LocalDialogBodyListProvider

@Composable
@Suppress("UnstableApiUsage")
fun PlainMessage(
    contents: Component,
    width: Long = 200L,
) {
    require(width in 1..1024) { "Width must be in [1, 1024]" }
    val bodyList = LocalDialogBodyListProvider.current
}
