package ink.pmc.essentials.screens.home

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.essentials.*
import ink.pmc.essentials.api.home.Home
import ink.pmc.essentials.api.home.HomeManager
import ink.pmc.framework.interactive.InteractiveScreen
import ink.pmc.framework.interactive.LocalPlayer
import ink.pmc.framework.interactive.canvas.Anvil
import ink.pmc.framework.chat.replace
import ink.pmc.framework.concurrent.submitAsync
import ink.pmc.framework.dsl.itemStack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.wesjd.anvilgui.AnvilGUI
import org.bukkit.Material
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.seconds

private enum class RenameState {
    NONE, INVALID, TOO_LONG, EXISTED, SUCCEED
}

class HomeEditorRenameScreen(private val home: Home) : InteractiveScreen() {
    @Composable
    override fun Content() {
        val player = LocalPlayer.current
        val coroutineScope = rememberCoroutineScope()
        var state by remember { mutableStateOf(RenameState.NONE) }
        val navigator = LocalNavigator.currentOrThrow
        val manager = koinInject<HomeManager>()

        fun stateTransition(newState: RenameState, pop: Boolean = false) {
            coroutineScope.launch {
                val keep = state
                state = newState
                delay(1.seconds)
                if (!pop) state = keep
                if (pop) navigator.pop()
            }
        }

        Anvil(
            title = UI_HOME_EDITOR_RENAME_TITLE.replace("<name>", home.name),
            text = home.name,
            left = itemStack(Material.YELLOW_STAINED_GLASS_PANE) {
                lore(UI_HOME_EDITOR_RENAME_EXIT_LORE)
            },
            right = itemStack(Material.GRAY_STAINED_GLASS_PANE) {
                meta {
                    isHideTooltip = true
                }
            },
            output = itemStack(Material.PAPER) {
                lore(
                    when (state) {
                        RenameState.NONE -> UI_HOME_EDITOR_RENAME_SAVE_EDITING(home)
                        RenameState.INVALID -> UI_HOME_EDITOR_RENAME_SAVE_INVALID_LORE
                        RenameState.TOO_LONG -> UI_HOME_EDITOR_RENAME_SAVE_TOO_LONG
                        RenameState.EXISTED -> UI_HOME_EDITOR_RENAME_SAVE_EXISTED
                        RenameState.SUCCEED -> UI_HOME_EDITOR_RENAME_SAVED
                    }
                )
                meta {
                    setEnchantmentGlintOverride(
                        when (state) {
                            RenameState.NONE -> false
                            else -> true
                        }
                    )
                }
            },
            onClick = { s, r ->
                when (s) {
                    AnvilGUI.Slot.INPUT_LEFT -> {
                        navigator.pop()
                        emptyList()
                    }

                    AnvilGUI.Slot.OUTPUT -> {
                        if (state != RenameState.NONE) return@Anvil emptyList()
                        val input = r.text

                        if (input.length > manager.nameLengthLimit) {
                            player.playSound(UI_HOME_EDITOR_RENAME_INVALID_SOUND)
                            stateTransition(RenameState.TOO_LONG)
                            return@Anvil emptyList()
                        }

                        coroutineScope.launch {
                            if (manager.has(player, input)) {
                                player.playSound(UI_HOME_EDITOR_RENAME_INVALID_SOUND)
                                stateTransition(RenameState.EXISTED)
                                return@launch
                            }

                            submitAsync {
                                home.name = input
                                home.update()
                            }

                            stateTransition(RenameState.SUCCEED, true)
                            player.playSound(UI_HOME_EDIT_SUCCEED_SOUND)
                        }

                        emptyList()
                    }

                    else -> emptyList()
                }
            }
        )
    }
}