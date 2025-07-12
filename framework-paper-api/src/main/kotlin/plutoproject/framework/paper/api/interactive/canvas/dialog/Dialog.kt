package plutoproject.framework.paper.api.interactive.canvas.dialog

import androidx.compose.runtime.*
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.text.Component
import plutoproject.framework.paper.api.interactive.ComposableFunction
import plutoproject.framework.paper.api.interactive.LocalPlayer

val LocalDialogBodyListProvider: ProvidableCompositionLocal<MutableList<DialogBody>> =
    staticCompositionLocalOf { error("Unexpected") }

val LocalDialogInputListProvider: ProvidableCompositionLocal<MutableList<DialogInput>> =
    staticCompositionLocalOf { error("Unexpected") }

@Composable
@Suppress("UnstableApiUsage")
fun Dialog(
    type: DialogType,
    title: Component = Component.empty(),
    externalTitle: Component = Component.empty(),
    canCloseWithEscape: Boolean = true,
    pause: Boolean = true,
    afterAction: DialogBase.DialogAfterAction = DialogBase.DialogAfterAction.CLOSE,
    body: ComposableFunction = {},
    input: ComposableFunction = {},
) {
    val player = LocalPlayer.current

    val bodyList = remember { mutableListOf<DialogBody>() }
    val inputList = remember { mutableListOf<DialogInput>() }

    CompositionLocalProvider(
        LocalDialogBodyListProvider provides bodyList,
        LocalDialogInputListProvider provides inputList,
    ) {
        body()
        input()
    }

    val dialog = remember(
        title, externalTitle, canCloseWithEscape, pause, afterAction, body, input, bodyList, inputList
    ) {
        val dialogBase = DialogBase.builder(title)
            .externalTitle(externalTitle)
            .canCloseWithEscape(canCloseWithEscape)
            .pause(pause)
            .afterAction(afterAction)
            .body(bodyList)
            .inputs(inputList)
            .build()
        Dialog.create {
            it.empty().base(dialogBase).type(type)
        }
    }

    player.showDialog(dialog)
}
