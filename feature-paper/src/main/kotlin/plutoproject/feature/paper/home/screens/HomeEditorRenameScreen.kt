package plutoproject.feature.paper.home.screens

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.wesjd.anvilgui.AnvilGUI.Slot.INPUT_LEFT
import net.wesjd.anvilgui.AnvilGUI.Slot.OUTPUT
import org.bukkit.Material
import plutoproject.feature.paper.api.home.Home
import plutoproject.feature.paper.api.home.HomeManager
import plutoproject.feature.paper.home.*
import plutoproject.framework.common.util.chat.UI_FAILED_SOUND
import plutoproject.framework.common.util.chat.UI_SUCCEED_SOUND
import plutoproject.framework.common.util.chat.component.replace
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.paper.api.interactive.InteractiveScreen
import plutoproject.framework.paper.api.interactive.LocalGuiScope
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.canvas.Anvil
import plutoproject.framework.paper.util.dsl.ItemStack
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

        val scope = LocalGuiScope.current
        player.send {
            text("该功能正在维护，请改用指令") with mochaMaroon
        }
        SideEffect {
            scope.dispose()
        }
        return

        // TODO: 修复铁砧

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
            left = ItemStack(Material.YELLOW_STAINED_GLASS_PANE) {
                lore(UI_HOME_EDITOR_RENAME_EXIT_LORE)
            },
            right = ItemStack(Material.GRAY_STAINED_GLASS_PANE) {
                meta {
                    isHideTooltip = true
                }
            },
            output = ItemStack(Material.PAPER) {
                lore(
                    when (state) {
                        RenameState.NONE -> getUIHomeEditorRenameSaveLore(home)
                        RenameState.INVALID -> UI_HOME_EDITOR_RENAME_SAVE_FAILED_INVALID_LORE
                        RenameState.TOO_LONG -> UI_HOME_EDITOR_RENAME_SAVE_FAILED_LENGTH_LIMIT_LORE
                        RenameState.EXISTED -> UI_HOME_EDITOR_RENAME_SAVE_FAILED_EXISTED_LORE
                        RenameState.SUCCEED -> UI_HOME_EDITOR_RENAME_SAVED_LORE
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
                    INPUT_LEFT -> {
                        navigator.pop()
                        emptyList()
                    }

                    OUTPUT -> {
                        if (state != RenameState.NONE) return@Anvil emptyList()
                        val input = r.text

                        if (input.length > HomeManager.nameLengthLimit) {
                            player.playSound(UI_FAILED_SOUND)
                            stateTransition(RenameState.TOO_LONG)
                            return@Anvil emptyList()
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

                        emptyList()
                    }

                    else -> emptyList()
                }
            }
        )
    }
}
