package ink.pmc.essentials.screens.warp

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.essentials.api.warp.Warp
import ink.pmc.essentials.api.warp.WarpCategory.*
import ink.pmc.essentials.api.warp.WarpManager
import ink.pmc.framework.chat.*
import ink.pmc.framework.interactive.LocalPlayer
import ink.pmc.framework.interactive.Item
import ink.pmc.framework.interactive.ItemSpacer
import ink.pmc.framework.interactive.Modifier
import ink.pmc.framework.interactive.click.clickable
import ink.pmc.framework.interactive.layout.list.ListMenu
import ink.pmc.framework.concurrent.sync
import ink.pmc.framework.time.formatDate
import ink.pmc.framework.time.zoneId
import ink.pmc.framework.world.aliasOrName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
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
                text("√ 已保存") with mochaGreen without italic()
            } else component {
                if (obj.alias != null) {
                    text("${obj.alias} ") with mochaYellow without italic()
                    text("(${obj.name})") with mochaSubtext0 without italic()
                } else {
                    text(obj.name) with mochaYellow without italic()
                }
            },
            lore = if (model.isPreferredSet) emptyList() else buildList {
                when (obj.category) {
                    MACHINE -> add(component {
                        text("\uD83D\uDD27 机械类") with mochaTeal without italic()
                    })

                    ARCHITECTURE -> add(component {
                        text("\uD83D\uDDFC 建筑类") with mochaFlamingo without italic()
                    })

                    TOWN -> add(component {
                        text("\uD83D\uDE84 城镇类") with mochaPeach without italic()
                    })

                    null -> {}
                }
                if (founderName != null) {
                    add(component {
                        text("由 $founderName") with mochaSubtext0 without italic()
                    })
                }
                add(component {
                    val time = ZonedDateTime.ofInstant(obj.createdAt, player.zoneId).formatDate()
                    text("设于 $time") with mochaSubtext0 without italic()
                })
                add(component {
                    val world = obj.location.world.aliasOrName
                    val x = obj.location.blockX
                    val y = obj.location.blockY
                    val z = obj.location.blockZ
                    text("$world $x, $y, $z") with mochaSubtext0 without italic()
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
                    text("左键 ") with mochaLavender without italic()
                    text("设为首选") with mochaText without italic()
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
                            if (!navigator.pop()) sync {
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