package plutoproject.feature.paper.home.screens

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.wesjd.anvilgui.AnvilGUI.Slot.*
import org.bukkit.Material
import plutoproject.feature.paper.api.home.HomeManager
import plutoproject.feature.paper.home.*
import plutoproject.framework.common.util.chat.UI_FAILED_SOUND
import plutoproject.framework.common.util.chat.UI_SUCCEED_SOUND
import plutoproject.framework.common.util.chat.palettes.mochaMaroon
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.paper.api.interactive.InteractiveScreen
import plutoproject.framework.paper.api.interactive.LocalGuiScope
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.canvas.Anvil
import plutoproject.framework.paper.util.dsl.ItemStack
import kotlin.time.Duration.Companion.seconds

class HomeCreatorScreen : InteractiveScreen() {
    @Composable
    override fun Content() {
        val player = LocalPlayer.current
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

        val scope = LocalGuiScope.current
        player.send {
            text("该功能正在维护，请改用指令") with mochaMaroon
        }
        SideEffect {
            scope.dispose()
        }
        return

        // TODO: 修复铁砧

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
            left = ItemStack(Material.YELLOW_STAINED_GLASS_PANE) {
                lore(UI_HOME_CREATOR_EXIT_LORE)
            },
            right = ItemStack(Material.GRAY_STAINED_GLASS_PANE) {
                meta {
                    isHideTooltip = true
                }
            },
            output = ItemStack(Material.PAPER) {
                lore(
                    when (state) {
                        0 -> getUIHomeCreatorOutputLore(player.location)
                        1 -> UI_HOME_EDITOR_RENAME_SAVE_FAILED_INVALID_LORE
                        2 -> UI_HOME_EDITOR_RENAME_SAVE_FAILED_LENGTH_LIMIT_LORE
                        3 -> UI_HOME_EDITOR_RENAME_SAVE_FAILED_EXISTED_LORE
                        4 -> UI_HOME_EDITOR_RENAME_SAVED_LORE
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

                        if (input.length > HomeManager.nameLengthLimit) {
                            player.playSound(UI_FAILED_SOUND)
                            stateTransition(2)
                            return@Anvil emptyList()
                        }

                        coroutineScope.launch {
                            if (HomeManager.has(player, input)) {
                                player.playSound(UI_FAILED_SOUND)
                                stateTransition(3)
                                return@launch
                            }

                            runAsync {
                                HomeManager.create(player, input, player.location)
                            }

                            stateTransition(4, true)
                            player.playSound(UI_SUCCEED_SOUND)
                        }

                        emptyList()
                    }

                    else -> error("Unsupported slot")
                }
            }
        )
    }
}
