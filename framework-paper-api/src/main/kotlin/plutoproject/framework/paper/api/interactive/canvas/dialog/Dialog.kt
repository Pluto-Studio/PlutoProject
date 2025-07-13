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
import plutoproject.framework.paper.util.entity.clearDialog

val LocalDialogBodyListProvider: ProvidableCompositionLocal<MutableList<DialogElement<DialogBody>>> =
    staticCompositionLocalOf { error("Unexpected") }

val LocalDialogInputListProvider: ProvidableCompositionLocal<MutableList<DialogElement<DialogInput>>> =
    staticCompositionLocalOf { error("Unexpected") }

@Composable
@Suppress("UnstableApiUsage")
fun Dialog(
    type: DialogType,
    title: Component = Component.empty(),
    externalTitle: Component = Component.empty(),
    canCloseWithEscape: Boolean = true,
    pause: Boolean = false,
    afterAction: DialogBase.DialogAfterAction = DialogBase.DialogAfterAction.NONE,
    body: ComposableFunction = {},
    input: ComposableFunction = {},
) {
    require(!pause || afterAction != DialogBase.DialogAfterAction.NONE) { "Pause cannot be enabled when after action is NONE" }

    val player = LocalPlayer.current
    val bodyList = mutableStateListOf<DialogElement<DialogBody>>()
    val inputList = mutableStateListOf<DialogElement<DialogInput>>()

    DisposableEffect(Unit) {
        onDispose {
            player.clearDialog()
        }
    }

    CompositionLocalProvider(
        LocalDialogBodyListProvider provides bodyList,
        LocalDialogInputListProvider provides inputList,
    ) {
        body()
        input()
    }

    val dialogBase = DialogBase.builder(title)
        .externalTitle(externalTitle)
        .canCloseWithEscape(canCloseWithEscape)
        .pause(pause)
        .afterAction(afterAction)
        .body(bodyList.map { it.element })
        .inputs(inputList.map { it.element })
        .build()

    val dialog = Dialog.create {
        it.empty().base(dialogBase).type(type)
    }

    player.showDialog(dialog)
}
