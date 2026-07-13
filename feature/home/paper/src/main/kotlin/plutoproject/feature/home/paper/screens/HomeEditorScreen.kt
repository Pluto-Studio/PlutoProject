package plutoproject.feature.home.paper.screens

import plutoproject.feature.home.paper.homeManager

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.replace
import ink.pmc.advkt.component.text
import ink.pmc.advkt.component.translatable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.home.api.paper.Home
import plutoproject.feature.home.api.paper.HomeManager
import plutoproject.feature.home.paper.*
import plutoproject.foundation.common.text.UI_FAILED_SOUND
import plutoproject.foundation.common.text.UI_SUCCEED_SOUND
import plutoproject.foundation.common.text.*
import plutoproject.feature.home.paper.moduleScope
import plutoproject.capability.interactive.api.InteractiveScreen
import plutoproject.capability.interactive.api.LocalPlayer
import plutoproject.capability.interactive.api.canvas.Menu
import plutoproject.capability.interactive.api.click.clickable
import plutoproject.capability.interactive.api.components.Item
import plutoproject.capability.interactive.api.components.ItemSpacer
import plutoproject.capability.interactive.api.layout.Row
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.capability.interactive.api.modifiers.fillMaxHeight
import plutoproject.capability.interactive.api.modifiers.width
import plutoproject.feature.home.paper.coroutineContext
import plutoproject.foundation.paper.inventory.addItemOrDrop
import kotlin.time.Duration.Companion.seconds

private enum class PreferState {
    NOT_PREFERRED, PREFERRED, SUCCEED
}

private enum class StarState {
    NOT_STARRED, STARRED, SUCCEED
}

private enum class SetIconState {
    NONE, NO_ITEM, SUCCEED
}

class HomeEditorScreen(private val home: Home) : InteractiveScreen() {
    @Composable
    override fun Content() {
        Menu(
            title = UI_HOME_EDITOR_TITLE.replace("<name>", home.name),
            rows = 3,
            centerBackground = true,
        ) {
            Row(modifier = Modifier.fillMaxHeight().width(7)) {
                Prefer()
                Star()
                Rename()
                SetIcon()
                Move()
                ItemSpacer()
                Delete()
            }
        }
    }

    @Composable
    @Suppress("FunctionName")
    private fun Prefer() {
        val player = LocalPlayer.current
        val coroutineScope = rememberCoroutineScope()
        var state by remember {
            mutableStateOf(if (!home.isPreferred) PreferState.NOT_PREFERRED else PreferState.PREFERRED)
        }

        fun stateTransition(newState: PreferState) {
            coroutineScope.launch {
                state = PreferState.SUCCEED
                delay(1.seconds)
                state = newState
            }
            player.playSound(UI_SUCCEED_SOUND)
        }

        Item(
            material = Material.SUNFLOWER,
            name = when (state) {
                PreferState.NOT_PREFERRED -> UI_HOME_EDITOR_SET_PREFER
                PreferState.PREFERRED -> UI_HOME_EDITOR_UNSET_PREFER
                PreferState.SUCCEED -> UI_HOME_EDITOR_SAVED
            },
            lore = when (state) {
                PreferState.NOT_PREFERRED -> UI_HOME_EDITOR_SET_PREFER_LORE
                PreferState.PREFERRED -> UI_HOME_EDITOR_UNSET_PREFER_LORE
                PreferState.SUCCEED -> emptyList()
            },
            enchantmentGlint = state == PreferState.SUCCEED || state == PreferState.PREFERRED,
            modifier = Modifier.clickable {
                if (!home.isLoaded) return@clickable
                if (state == PreferState.SUCCEED) return@clickable
                if (clickType != ClickType.LEFT) return@clickable

                if (home.isPreferred) {
                    moduleScope.launch(Dispatchers.IO) {
                        home.setPreferred(false)
                    }
                    stateTransition(PreferState.NOT_PREFERRED)
                    return@clickable
                }

                moduleScope.launch(Dispatchers.IO) {
                    home.setPreferred(true)
                }
                stateTransition(PreferState.PREFERRED)
            }
        )
    }

    @Composable
    @Suppress("FunctionName")
    private fun Star() {
        val player = LocalPlayer.current
        val coroutineScope = rememberCoroutineScope()
        var state by remember {
            mutableStateOf(if (!home.isStarred) StarState.NOT_STARRED else StarState.STARRED)
        }

        fun stateTransition(newState: StarState) {
            coroutineScope.launch {
                state = StarState.SUCCEED
                delay(1.seconds)
                state = newState
            }
            player.playSound(UI_SUCCEED_SOUND)
        }

        Item(
            material = Material.NETHER_STAR,
            name = when (state) {
                StarState.NOT_STARRED -> UI_HOME_EDITOR_SET_STAR
                StarState.STARRED -> UI_HOME_EDITOR_UNSET_STAR
                StarState.SUCCEED -> UI_HOME_EDITOR_SAVED
            },
            lore = when (state) {
                StarState.NOT_STARRED -> UI_HOME_EDITOR_SET_STAR_LORE
                StarState.STARRED -> UI_HOME_EDITOR_UNSET_STAR_LORE
                StarState.SUCCEED -> emptyList()
            },
            enchantmentGlint = state == StarState.SUCCEED || state == StarState.STARRED,
            modifier = Modifier.clickable {
                if (!home.isLoaded) return@clickable
                if (state == StarState.SUCCEED) return@clickable
                if (clickType != ClickType.LEFT) return@clickable

                if (home.isStarred) {
                    moduleScope.launch(Dispatchers.IO) {
                        home.isStarred = false
                        home.update()
                    }
                    stateTransition(StarState.NOT_STARRED)
                    return@clickable
                }

                moduleScope.launch(Dispatchers.IO) {
                    home.isStarred = true
                    home.update()
                }
                stateTransition(StarState.STARRED)
            }
        )
    }

    @Composable
    @Suppress("FunctionName")
    private fun Rename() {
        val navigator = LocalNavigator.currentOrThrow
        val succeed by remember { mutableStateOf(false) }
        Item(
            material = Material.NAME_TAG,
            name = if (!succeed) UI_HOME_EDITOR_RENAME else UI_HOME_EDITOR_SAVED,
            lore = if (!succeed) UI_HOME_EDITOR_RENAME_LORE else emptyList(),
            enchantmentGlint = succeed,
            modifier = Modifier.clickable {
                if (!home.isLoaded) return@clickable
                if (clickType != ClickType.LEFT) return@clickable
                if (succeed) return@clickable
                navigator.push(HomeEditorRenameScreen(home))
            }
        )
    }

    @Composable
    @Suppress("FunctionName")
    private fun Move() {
        val player = LocalPlayer.current
        var succeed by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        Item(
            material = Material.COMPASS,
            name = if (!succeed) UI_HOME_EDITOR_MOVE else UI_HOME_EDITOR_SAVED,
            lore = if (!succeed) UI_HOME_EDITOR_MOVE_LORE else emptyList(),
            enchantmentGlint = succeed,
            modifier = Modifier.clickable {
                if (!home.isLoaded) return@clickable
                if (clickType != ClickType.LEFT || succeed) return@clickable
                home.location = player.location
                moduleScope.launch(Dispatchers.IO) {
                    home.update()
                }
                whoClicked.playSound(UI_SUCCEED_SOUND)
                coroutineScope.launch {
                    succeed = true
                    delay(1.seconds)
                    succeed = false
                }
            }
        )
    }

    @Composable
    @Suppress("FunctionName")
    private fun Delete() {
        val navigator = LocalNavigator.currentOrThrow
        val coroutineScope = rememberCoroutineScope()
        Item(
            material = Material.RED_STAINED_GLASS_PANE,
            name = UI_HOME_EDITOR_DELETE,
            lore = UI_HOME_EDITOR_DELETE_LORE,
            modifier = Modifier.clickable {
                if (!home.isLoaded) return@clickable
                if (clickType != ClickType.SHIFT_LEFT) return@clickable
                coroutineScope.launch {
                    homeManager.remove(home.id)
                    whoClicked.playSound(UI_HOME_EDITOR_DELETE_SOUND)
                    navigator.pop()
                }
            }
        )
    }

    @Composable
    @Suppress("FunctionName")
    private fun SetIcon() {
        val player = LocalPlayer.current
        val coroutineScope = rememberCoroutineScope()
        var current by remember { mutableStateOf(home.icon) }
        var state by remember { mutableStateOf(SetIconState.NONE) }
        Item(
            material = if (state == SetIconState.SUCCEED && current != null) current!! else Material.ITEM_FRAME,
            name = if (state == SetIconState.SUCCEED) component {
                text("√ 已保存") with mochaGreen
            } else component {
                text("设置图标") with mochaText
            },
            enchantmentGlint = state == SetIconState.SUCCEED,
            lore = when (state) {
                SetIconState.NONE -> buildList {
                    add(component {
                        text("将物品放置在此处以设置图标") with mochaSubtext0
                    })
                    if (current != null) {
                        add(component {
                            text("当前设置 ") with mochaSubtext0
                            translatable(current!!.translationKey()) with mochaText
                        })
                    }
                    add(Component.empty())
                    add(component {
                        text("左键 ") with mochaLavender
                        text("设置图标") with mochaText
                    })
                    add(component {
                        text("Shift + 左键 ") with mochaLavender
                        text("恢复默认") with mochaText
                    })
                }

                SetIconState.NO_ITEM -> buildList {
                    add(component {
                        text("请将物品放置在此处") with mochaMaroon
                    })
                }

                SetIconState.SUCCEED -> emptyList()
            },
            modifier = Modifier.clickable {
                if (state != SetIconState.NONE) return@clickable
                when (clickType) {
                    ClickType.LEFT -> {
                        val carriedItem = cursor
                        val material = cursor?.type
                        if (material == null) {
                            state = SetIconState.NO_ITEM
                            player.playSound(UI_FAILED_SOUND)
                            coroutineScope.launch {
                                delay(1.seconds)
                                state = SetIconState.NONE
                            }
                            return@clickable
                        }

                        home.icon = material
                        home.update()
                        current = material
                        withContext(player.coroutineContext) {
                            view.setCursor(null)
                            player.inventory.addItemOrDrop(carriedItem!!)
                        }
                        state = SetIconState.SUCCEED
                        player.playSound(UI_SUCCEED_SOUND)
                        coroutineScope.launch {
                            delay(1.seconds)
                            state = SetIconState.NONE
                        }
                    }

                    ClickType.SHIFT_LEFT -> {
                        home.icon = null
                        home.update()
                        current = null
                        state = SetIconState.SUCCEED
                        player.playSound(UI_SUCCEED_SOUND)
                        coroutineScope.launch {
                            delay(1.seconds)
                            state = SetIconState.NONE
                        }
                    }

                    else -> {}
                }
            }
        )
    }
}
