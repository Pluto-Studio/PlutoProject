package plutoproject.feature.home.paper.screens

import plutoproject.feature.home.paper.homeManager

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import plutoproject.feature.home.api.paper.HomeManager
import plutoproject.feature.home.paper.*
import plutoproject.foundation.common.text.UI_FAILED_SOUND
import plutoproject.foundation.common.text.UI_SUCCEED_SOUND
import plutoproject.feature.home.paper.moduleScope
import plutoproject.capability.interactive.api.InteractiveScreen
import plutoproject.capability.interactive.api.LocalPlayer
import plutoproject.capability.interactive.api.canvas.dialog.body.PlainMessageBody
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

                if (input.length > homeManager.nameLengthLimit) {
                    player.playSound(UI_FAILED_SOUND)
                    stateTransition(CreateState.TOO_LONG)
                    return@HomeNameDialog
                }

                coroutineScope.launch {
                    if (homeManager.has(player, input)) {
                        player.playSound(UI_FAILED_SOUND)
                        stateTransition(CreateState.EXISTED)
                        return@launch
                    }

                    moduleScope.launch(Dispatchers.IO) {
                        homeManager.create(player, input, player.location)
                    }

                    stateTransition(CreateState.SUCCEED, true)
                    player.playSound(UI_SUCCEED_SOUND)
                }
            },
        )
    }
}
