package plutoproject.feature.paper.warp.screens

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.paper.api.warp.Warp
import plutoproject.feature.paper.api.warp.WarpCategory
import plutoproject.feature.paper.api.warp.WarpManager
import plutoproject.framework.common.util.chat.UI_SUCCEED_SOUND
import plutoproject.framework.common.util.chat.component.splitLines
import plutoproject.framework.common.util.chat.palettes.*
import plutoproject.framework.common.util.time.formatDate
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.components.ItemSpacer
import plutoproject.framework.paper.api.interactive.layout.list.ListMenu
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.provider.timezone
import plutoproject.framework.paper.api.worldalias.aliasOrName
import plutoproject.framework.paper.util.coroutine.coroutineContext
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.seconds

class DefaultSpawnPickerScreen : ListMenu<Warp, DefaultSpawnPickerScreenModel>() {
    @Composable
    override fun MenuLayout() {
        LocalListMenuOptions.current.title = Component.text("选择主城")
        super.MenuLayout()
    }

    @Composable
    override fun modelProvider(): DefaultSpawnPickerScreenModel {
        return DefaultSpawnPickerScreenModel()
    }

    @Composable
    override fun Element(obj: Warp) {
        val model = LocalListMenuModel.current
        val options = LocalListMenuOptions.current
        val coroutineScope = rememberCoroutineScope()
        val navigator = LocalNavigator.currentOrThrow
        val player = LocalPlayer.current
        var founderName by rememberSaveable(obj) { mutableStateOf<String?>(null) }

        if (model.isPreferredSet && model.preferredSet != obj) {
            ItemSpacer()
            return
        }

        if (obj.founder != null) {
            LaunchedEffect(obj) {
                founderName = obj.founder?.let {
                    val founder = it.await()
                    founder.name
                }
            }
        }

        Item(
            material = obj.icon ?: Material.PAPER,
            name = if (model.isPreferredSet) component {
                text("√ 已保存") with mochaGreen
            } else component {
                if (obj.alias != null) {
                    text("${obj.alias} ") with mochaYellow
                    text("(${obj.name})") with mochaSubtext0
                } else {
                    text(obj.name) with mochaYellow
                }
            },
            lore = if (model.isPreferredSet) emptyList() else buildList {
                when (obj.category) {
                    WarpCategory.MACHINE -> add(component {
                        text("\uD83D\uDD27 机械类") with mochaTeal
                    })

                    WarpCategory.ARCHITECTURE -> add(component {
                        text("\uD83D\uDDFC 建筑类") with mochaFlamingo
                    })

                    WarpCategory.TOWN -> add(component {
                        text("\uD83D\uDE84 城镇类") with mochaPeach
                    })

                    null -> {}
                }
                if (founderName != null) {
                    add(component {
                        text("由 $founderName") with mochaSubtext0
                    })
                }
                add(component {
                    val time = ZonedDateTime.ofInstant(obj.createdAt, player.timezone.toZoneId()).formatDate()
                    text("设于 $time") with mochaSubtext0
                })
                add(component {
                    val world = obj.location.world.aliasOrName
                    val x = obj.location.blockX
                    val y = obj.location.blockY
                    val z = obj.location.blockZ
                    text("$world $x, $y, $z") with mochaSubtext0
                })
                obj.description?.let {
                    add(Component.empty())
                    addAll(it.splitLines().map { line ->
                        line.colorIfAbsent(mochaSubtext0)
                            .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                    })
                }
                add(Component.empty())
                add(component {
                    text("左键 ") with mochaLavender
                    text("设为首选") with mochaText
                })
            },
            modifier = Modifier.clickable {
                when (clickType) {
                    ClickType.LEFT -> {
                        if (model.isPreferredSet || model.preferredSet != null) return@clickable
                        WarpManager.setPreferredSpawn(player, obj)
                        model.isPreferredSet = true
                        model.preferredSet = obj
                        options.centerBackground = true
                        coroutineScope.launch {
                            delay(1.seconds)
                            if (!navigator.pop()) withContext(player.coroutineContext) {
                                player.closeInventory()
                            }
                        }
                        player.playSound(UI_SUCCEED_SOUND)
                    }

                    else -> {}
                }
            }
        )
    }
}
