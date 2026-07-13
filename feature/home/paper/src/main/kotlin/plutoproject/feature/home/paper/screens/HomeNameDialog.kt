package plutoproject.feature.home.paper.screens

import plutoproject.feature.home.paper.homeManager

import androidx.compose.runtime.*
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import org.bukkit.entity.Player
import plutoproject.feature.home.api.paper.HomeManager
import plutoproject.feature.home.paper.UI_DIALOG_NAME_INPUT_CANCEL
import plutoproject.feature.home.paper.UI_DIALOG_NAME_INPUT_SUBMIT
import plutoproject.feature.home.paper.UI_DIALOG_NAME_INPUT_TEXT_INPUT_LABEL
import plutoproject.capability.interactive.api.ComposableFunction
import plutoproject.capability.interactive.api.canvas.dialog.Dialog
import plutoproject.capability.interactive.api.canvas.dialog.input.TextInput

@Composable
@Suppress("UnstableApiUsage")
fun HomeNameDialog(
    title: Component,
    body: ComposableFunction = {},
    showInput: Boolean = true,
    initialInput: String = "",
    onCancel: (Player) -> Unit,
    onSubmit: (String) -> Unit,
) {
    var submittedText by remember { mutableStateOf(initialInput) }

    val cancelCallback = DialogAction.customClick({ _, audience ->
        val player = audience as Player
        onCancel(player)
    }, ClickCallback.Options.builder().build())
    val submitCallback = DialogAction.customClick({ view, _ ->
        val text = view.getText("name_input") ?: return@customClick
        submittedText = text
        onSubmit(text)
    }, ClickCallback.Options.builder().build())

    val noButton = ActionButton.builder(UI_DIALOG_NAME_INPUT_CANCEL).action(cancelCallback).build()
    val yesButton = ActionButton.builder(UI_DIALOG_NAME_INPUT_SUBMIT).action(submitCallback).build()

    Dialog(
        type = DialogType.confirmation(noButton, yesButton),
        canCloseWithEscape = false,
        title = title,
        body = body,
        input = {
            if (!showInput) return@Dialog
            TextInput(
                key = "name_input",
                label = UI_DIALOG_NAME_INPUT_TEXT_INPUT_LABEL,
                maxLength = homeManager.nameLengthLimit,
                initial = submittedText
            )
        },
    )
}
