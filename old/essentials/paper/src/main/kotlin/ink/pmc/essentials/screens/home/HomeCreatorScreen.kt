package ink.pmc.essentials.screens.home

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.essentials.*
import ink.pmc.essentials.api.home.HomeManager
import ink.pmc.framework.interactive.InteractiveScreen
import ink.pmc.framework.interactive.LocalPlayer
import ink.pmc.framework.interactive.canvas.Anvil
import ink.pmc.framework.concurrent.submitAsync
import ink.pmc.framework.dsl.itemStack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.wesjd.anvilgui.AnvilGUI.Slot.*
import org.bukkit.Material
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.seconds

class HomeCreatorScreen : InteractiveScreen() {
    @Composable
    override fun Content() {
        val player = LocalPlayer.current
        val manager = koinInject<HomeManager>()
        val navigator = LocalNavigator.currentOrThrow
        val coroutineScope = rememberCoroutineScope()
        /*w
        * 0 -> 编辑中
        * 1 -> 包含不允许的字符
        * 2 -> 名称过长
        * 3 -> 已有同名家
        * 4 -> 保存成功
        * */
        var state by remember { mutableStateOf(0) }

        fun stateTransition(newState: Int, pop: Boolean = false) {
            coroutineScope.launch {
                val keep = state
                state = newState
                delay(1.seconds)
                if (!pop) state = keep
                if (pop) navigator.pop()
            }
        }

        Anvil(
            title = UI_HOME_CREATOR_TITLE,
            left = itemStack(Material.YELLOW_STAINED_GLASS_PANE) {
                lore(UI_HOME_CREATOR_LEFT_LORE)
            },
            right = itemStack(Material.GRAY_STAINED_GLASS_PANE) {
                meta {
                    isHideTooltip = true
                }
            },
            output = itemStack(Material.PAPER) {
                lore(
                    when (state) {
                        0 -> UI_HOME_CREATOR_OUTPUT_LORE(player.location)
                        1 -> UI_HOME_EDITOR_RENAME_SAVE_INVALID_LORE
                        2 -> UI_HOME_EDITOR_RENAME_SAVE_TOO_LONG
                        3 -> UI_HOME_EDITOR_RENAME_SAVE_EXISTED
                        4 -> UI_HOME_EDITOR_RENAME_SAVED
                        else -> error("Unsupported state")
                    }
                )
                meta {
                    setEnchantmentGlintOverride(state > 0)
                }
            },
            text = UI_HOME_CREATOR_INPUT,
            onClick = { s, r ->
                when (s) {
                    INPUT_LEFT -> {
                        navigator.pop()
                        emptyList()
                    }

                    INPUT_RIGHT -> emptyList()
                    OUTPUT -> {
                        if (state != 0) return@Anvil emptyList()
                        val input = r.text

                        if (input.length > manager.nameLengthLimit) {
                            player.playSound(UI_HOME_EDITOR_RENAME_INVALID_SOUND)
                            stateTransition(2)
                            return@Anvil emptyList()
                        }

                        coroutineScope.launch {
                            if (manager.has(player, input)) {
                                player.playSound(UI_HOME_EDITOR_RENAME_INVALID_SOUND)
                                stateTransition(3)
                                return@launch
                            }

                            submitAsync {
                                manager.create(player, input, player.location)
                            }

                            stateTransition(4, true)
                            player.playSound(UI_HOME_EDIT_SUCCEED_SOUND)
                        }

                        emptyList()
                    }

                    else -> error("Unsupported slot")
                }
            }
        )
    }
}