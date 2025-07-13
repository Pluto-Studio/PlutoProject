package plutoproject.feature.paper.home.screens

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import io.papermc.paper.registry.data.dialog.body.DialogBody
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import plutoproject.feature.paper.api.home.Home
import plutoproject.feature.paper.api.home.HomeManager
import plutoproject.feature.paper.home.*
import plutoproject.framework.common.util.chat.UI_FAILED_SOUND
import plutoproject.framework.common.util.chat.UI_SUCCEED_SOUND
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.paper.api.interactive.InteractiveScreen
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.canvas.dialog.body.ItemBody
import plutoproject.framework.paper.api.interactive.canvas.dialog.body.PlainMessageBody
import kotlin.time.Duration.Companion.seconds

private enum class RenameState {
    NONE, EMPTY_NAME, TOO_LONG, EXISTED, SUCCEED
}

@Suppress("UnstableApiUsage")
class HomeEditorRenameScreen(private val home: Home) : InteractiveScreen() {
    @Composable
    override fun Content() {
        val player = LocalPlayer.current
        val coroutineScope = rememberCoroutineScope()
        var state by remember { mutableStateOf(RenameState.NONE) }
        val navigator = LocalNavigator.currentOrThrow

        fun stateTransition(newState: RenameState, pop: Boolean = false) {
            coroutineScope.launch {
                val keep = state
                state = newState
                delay(1.seconds)
                if (!pop) state = keep
                if (pop) navigator.pop()
            }
        }

        HomeNameDialog(
            title = UI_DIALOG_NAME_INPUT_TITLE_RENAME,
            body = {
                if (state == RenameState.NONE) {
                    val plainMessage = remember {
                        DialogBody.plainMessage(UI_DIALOG_NAME_INPUT_RENAMING.replace("<name>", home.name), 1024)
                    }
                    ItemBody(
                        item = ItemStack(Material.NAME_TAG),
                        description = plainMessage,
                        showDecorations = false,
                        showTooltip = false,
                    )
                    return@HomeNameDialog
                }

                val message = when (state) {
                    RenameState.EMPTY_NAME -> UI_DIALOG_NAME_INPUT_SAVE_FAILED_EMPTY_NAME
                    RenameState.TOO_LONG -> UI_DIALOG_NAME_INPUT_SAVE_FAILED_TOO_LONG
                    RenameState.EXISTED -> UI_DIALOG_NAME_INPUT_SAVE_FAILED_EXISTED
                    RenameState.SUCCEED -> UI_DIALOG_NAME_INPUT_SAVED
                    else -> error("Unexpected")
                }

                PlainMessageBody(message)
            },
            showInput = state == RenameState.NONE,
            initialInput = home.name,
            onCancel = {
                navigator.pop()
            },
            onSubmit = { input ->
                if (state != RenameState.NONE) {
                    return@HomeNameDialog
                }

                if (input.isBlank()) {
                    player.playSound(UI_FAILED_SOUND)
                    stateTransition(RenameState.EMPTY_NAME)
                    return@HomeNameDialog
                }

                if (input.length > HomeManager.nameLengthLimit) {
                    player.playSound(UI_FAILED_SOUND)
                    stateTransition(RenameState.TOO_LONG)
                    return@HomeNameDialog
                }

                if (input == home.name) {
                    navigator.pop()
                    return@HomeNameDialog
                }

                coroutineScope.launch {
                    if (HomeManager.has(player, input)) {
                        player.playSound(UI_FAILED_SOUND)
                        stateTransition(RenameState.EXISTED)
                        return@launch
                    }

                    runAsync {
                        home.name = input
                        home.update()
                    }

                    stateTransition(RenameState.SUCCEED, true)
                    player.playSound(UI_SUCCEED_SOUND)
                }
            },
        )
    }
}
