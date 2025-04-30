package plutoproject.feature.paper.home.screens

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.paper.api.home.Home
import plutoproject.feature.paper.api.home.HomeManager
import plutoproject.feature.paper.home.*
import plutoproject.framework.common.util.chat.UI_INVALID_SOUND
import plutoproject.framework.common.util.chat.UI_SUCCEED_SOUND
import plutoproject.framework.common.util.chat.palettes.*
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.paper.api.interactive.InteractiveScreen
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.canvas.Menu
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.components.ItemSpacer
import plutoproject.framework.paper.api.interactive.layout.Row
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.modifiers.fillMaxHeight
import plutoproject.framework.paper.api.interactive.modifiers.width
import plutoproject.framework.paper.util.coroutine.withSync
import plutoproject.framework.paper.util.inventory.addItemOrDrop
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
                ChangeLocation()
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
            player.playSound(UI_HOME_EDIT_SUCCEED_SOUND)
        }

        Item(
            material = Material.SUNFLOWER,
            name = when (state) {
                PreferState.NOT_PREFERRED -> UI_HOME_PREFER
                PreferState.PREFERRED -> UI_HOME_PREFER_UNSET
                PreferState.SUCCEED -> UI_HOME_EDIT_SUCCEED
            },
            lore = when (state) {
                PreferState.NOT_PREFERRED -> UI_HOME_PREFER_LORE
                PreferState.PREFERRED -> UI_HOME_PREFER_UNSET_LORE
                PreferState.SUCCEED -> emptyList()
            },
            enchantmentGlint = state == PreferState.SUCCEED || state == PreferState.PREFERRED,
            modifier = Modifier.clickable {
                if (!home.isLoaded) return@clickable
                if (state == PreferState.SUCCEED) return@clickable
                if (clickType != ClickType.LEFT) return@clickable

                if (home.isPreferred) {
                    runAsync { home.setPreferred(false) }
                    stateTransition(PreferState.NOT_PREFERRED)
                    return@clickable
                }

                runAsync { home.setPreferred(true) }
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
            player.playSound(UI_HOME_EDIT_SUCCEED_SOUND)
        }

        Item(
            material = Material.NETHER_STAR,
            name = when (state) {
                StarState.NOT_STARRED -> UI_HOME_STAR
                StarState.STARRED -> UI_HOME_STAR_UNSET
                StarState.SUCCEED -> UI_HOME_EDIT_SUCCEED
            },
            lore = when (state) {
                StarState.NOT_STARRED -> UI_HOME_STAR_LORE
                StarState.STARRED -> UI_HOME_STAR_UNSET_LORE
                StarState.SUCCEED -> emptyList()
            },
            enchantmentGlint = state == StarState.SUCCEED || state == StarState.STARRED,
            modifier = Modifier.clickable {
                if (!home.isLoaded) return@clickable
                if (state == StarState.SUCCEED) return@clickable
                if (clickType != ClickType.LEFT) return@clickable

                if (home.isStarred) {
                    runAsync {
                        home.isStarred = false
                        home.update()
                    }
                    stateTransition(StarState.NOT_STARRED)
                    return@clickable
                }

                runAsync {
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
            name = if (!succeed) UI_HOME_RENAME else UI_HOME_EDIT_SUCCEED,
            lore = if (!succeed) UI_HOME_RENAME_LORE else emptyList(),
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
    private fun ChangeLocation() {
        val player = LocalPlayer.current
        var succeed by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        Item(
            material = Material.COMPASS,
            name = if (!succeed) UI_HOME_CHANGE_LOCATION else UI_HOME_EDIT_SUCCEED,
            lore = if (!succeed) UI_HOME_CHANGE_LOCATION_LORE else emptyList(),
            enchantmentGlint = succeed,
            modifier = Modifier.clickable {
                if (!home.isLoaded) return@clickable
                if (clickType != ClickType.LEFT || succeed) return@clickable
                home.location = player.location
                runAsync { home.update() }
                whoClicked.playSound(UI_HOME_EDIT_SUCCEED_SOUND)
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
            name = UI_HOME_DELETE,
            lore = UI_HOME_DELETE_LORE,
            modifier = Modifier.clickable {
                if (!home.isLoaded) return@clickable
                if (clickType != ClickType.SHIFT_LEFT) return@clickable
                coroutineScope.launch {
                    HomeManager.remove(home.id)
                    whoClicked.playSound(UI_HOME_EDITOR_REMOVE_SOUND)
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
                text("√ 已保存") with mochaGreen without italic()
            } else component {
                text("设置图标") with mochaText without italic()
            },
            enchantmentGlint = state == SetIconState.SUCCEED,
            lore = when (state) {
                SetIconState.NONE -> buildList {
                    add(component {
                        text("将物品放置在此处以设置图标") with mochaSubtext0 without italic()
                    })
                    if (current != null) {
                        add(component {
                            text("当前设置 ") with mochaSubtext0 without italic()
                            translatable(current!!.translationKey()) with mochaText without italic()
                        })
                    }
                    add(Component.empty())
                    add(component {
                        text("左键 ") with mochaLavender without italic()
                        text("设置图标") with mochaText without italic()
                    })
                    add(component {
                        text("Shift + 左键 ") with mochaLavender without italic()
                        text("恢复默认") with mochaText without italic()
                    })
                }

                SetIconState.NO_ITEM -> buildList {
                    add(component {
                        text("请将物品放置在此处") with mochaMaroon without italic()
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
                            player.playSound(UI_INVALID_SOUND)
                            coroutineScope.launch {
                                delay(1.seconds)
                                state = SetIconState.NONE
                            }
                            return@clickable
                        }

                        home.icon = material
                        home.update()
                        current = material
                        player.withSync {
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
