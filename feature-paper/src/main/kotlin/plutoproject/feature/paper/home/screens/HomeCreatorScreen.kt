package plutoproject.feature.paper.home.screens

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import plutoproject.feature.paper.api.home.HomeManager
import plutoproject.feature.paper.home.*
import plutoproject.framework.common.util.chat.UI_FAILED_SOUND
import plutoproject.framework.common.util.chat.UI_SUCCEED_SOUND
import plutoproject.framework.common.util.coroutine.Loom
import plutoproject.framework.common.util.coroutine.PluginScope
import plutoproject.framework.paper.api.interactive.InteractiveScreen
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.canvas.dialog.body.PlainMessageBody
import kotlin.time.Duration.Companion.seconds

private enum class CreateState {
    NONE, EMPTY_NAME, TOO_LONG, EXISTED, SUCCEED
}

class HomeCreatorScreen : InteractiveScreen() {
    @Composable
    override fun Content() {
        val player = LocalPlayer.current
        val coroutineScope = rememberCoroutineScope()
        var state by remember { mutableStateOf(CreateState.NONE) }
        val navigator = LocalNavigator.currentOrThrow


        fun stateTransition(newState: CreateState, pop: Boolean = false) {
            coroutineScope.launch {
                val keep = state
                state = newState
                delay(1.seconds)
                if (!pop) state = keep
                if (pop) navigator.pop()
            }
        }

        HomeNameDialog(
            title = UI_DIALOG_NAME_INPUT_TITLE_CREATE,
            body = {
                val message = when (state) {
                    CreateState.NONE -> return@HomeNameDialog
                    CreateState.EMPTY_NAME -> UI_DIALOG_NAME_INPUT_SAVE_FAILED_EMPTY_NAME
                    CreateState.TOO_LONG -> UI_DIALOG_NAME_INPUT_SAVE_FAILED_TOO_LONG
                    CreateState.EXISTED -> UI_DIALOG_NAME_INPUT_SAVE_FAILED_EXISTED
                    CreateState.SUCCEED -> UI_DIALOG_NAME_INPUT_SAVED
                }

                PlainMessageBody(message)
            },
            showInput = state == CreateState.NONE,
            onCancel = {
                navigator.pop()
            },
            onSubmit = { input ->
                if (state != CreateState.NONE) {
                    return@HomeNameDialog
                }

                if (input.isBlank()) {
                    player.playSound(UI_FAILED_SOUND)
                    stateTransition(CreateState.EMPTY_NAME)
                    return@HomeNameDialog
                }

                if (input.length > HomeManager.nameLengthLimit) {
                    player.playSound(UI_FAILED_SOUND)
                    stateTransition(CreateState.TOO_LONG)
                    return@HomeNameDialog
                }

                coroutineScope.launch {
                    if (HomeManager.has(player, input)) {
                        player.playSound(UI_FAILED_SOUND)
                        stateTransition(CreateState.EXISTED)
                        return@launch
                    }

                    PluginScope.launch(Dispatchers.Loom) {
                        HomeManager.create(player, input, player.location)
                    }

                    stateTransition(CreateState.SUCCEED, true)
                    player.playSound(UI_SUCCEED_SOUND)
                }
            },
        )
    }
}
