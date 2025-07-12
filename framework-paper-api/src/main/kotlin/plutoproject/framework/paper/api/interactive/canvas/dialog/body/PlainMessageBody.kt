package plutoproject.framework.paper.api.interactive.canvas.dialog.body

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.papermc.paper.registry.data.dialog.body.DialogBody
import net.kyori.adventure.text.Component

@Composable
@Suppress("UnstableApiUsage")
fun PlainMessageBody(
    contents: Component,
    width: Int = 200,
) {
    BodyElement(remember(contents, width) {
        DialogBody.plainMessage(contents, width)
    })
}
