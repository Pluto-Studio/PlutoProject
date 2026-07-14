package plutoproject.feature.warp.paper.screens

import plutoproject.feature.warp.paper.warpManager

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
import plutoproject.feature.warp.api.paper.Warp
import plutoproject.feature.warp.api.paper.WarpCategory
import plutoproject.feature.warp.api.paper.WarpManager
import plutoproject.foundation.common.text.UI_SUCCEED_SOUND
import plutoproject.foundation.common.text.splitLines
import plutoproject.foundation.common.text.*
import plutoproject.foundation.common.time.formatDate
import plutoproject.capability.interactive.api.LocalPlayer
import plutoproject.capability.interactive.api.click.clickable
import plutoproject.capability.interactive.api.components.Item
import plutoproject.capability.interactive.api.components.ItemSpacer
import plutoproject.capability.interactive.api.layout.list.ListMenu
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.feature.warp.paper.timezone
import plutoproject.feature.warp.paper.aliasOrName
import plutoproject.feature.warp.paper.coroutineContext
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
                        warpManager.setPreferredSpawn(player, obj)
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
