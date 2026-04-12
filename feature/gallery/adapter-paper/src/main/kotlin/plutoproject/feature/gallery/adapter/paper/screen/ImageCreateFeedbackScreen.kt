package plutoproject.feature.gallery.adapter.paper.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.type.DialogType
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import plutoproject.feature.gallery.adapter.paper.IMAGE_CREATE_CANCEL
import plutoproject.feature.gallery.adapter.paper.IMAGE_CREATE_TITLE
import plutoproject.framework.paper.api.interactive.InteractiveScreen
import plutoproject.framework.paper.api.interactive.canvas.dialog.Dialog
import plutoproject.framework.paper.api.interactive.canvas.dialog.body.PlainMessageBody
import kotlin.time.Duration.Companion.seconds

internal class ImageCreateFeedbackScreen(
    private val message: Component,
) : InteractiveScreen() {
    @Composable
    @Suppress("UnstableApiUsage")
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        LaunchedEffect(Unit) {
            delay(1.seconds)
            navigator.pop()
        }

        val closeCallback = DialogAction.customClick(
            { _, _ -> navigator.pop() },
            ClickCallback.Options.builder().build()
        )
        val closeButton = ActionButton.builder(IMAGE_CREATE_CANCEL).action(closeCallback).build()

        Dialog(
            type = DialogType.confirmation(closeButton, closeButton),
            canCloseWithEscape = false,
            title = IMAGE_CREATE_TITLE,
            body = {
                PlainMessageBody(message)
            }
        )
    }
}
