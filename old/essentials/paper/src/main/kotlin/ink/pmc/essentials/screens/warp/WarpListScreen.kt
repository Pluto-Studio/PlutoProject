package ink.pmc.essentials.screens.warp

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.essentials.api.warp.Warp
import ink.pmc.essentials.api.warp.WarpCategory.*
import ink.pmc.essentials.api.warp.WarpManager
import ink.pmc.framework.chat.*
import ink.pmc.framework.interactive.LocalPlayer
import ink.pmc.framework.interactive.Item
import ink.pmc.framework.interactive.Modifier
import ink.pmc.framework.interactive.click.clickable
import ink.pmc.framework.interactive.layout.list.FilterListMenu
import ink.pmc.framework.concurrent.sync
import ink.pmc.framework.player.profile.ProfileCache
import ink.pmc.framework.time.formatDate
import ink.pmc.framework.time.zoneId
import ink.pmc.framework.world.aliasOrName
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import java.time.ZonedDateTime

class WarpListScreen : FilterListMenu<Warp, WarpFilter, WarpListScreenModel>(
    filters = WarpFilter.entries.associateWith { it.filterName }
) {
    @Composable
    override fun MenuLayout() {
        LocalListMenuOptions.current.title = Component.text("地标")
        super.MenuLayout()
    }

    @Composable
    override fun modelProvider(): WarpListScreenModel {
        val player = LocalPlayer.current
        return WarpListScreenModel(player)
    }

    @Composable
    override fun Element(obj: Warp) {
        val model = LocalListMenuModel.current
        val player = LocalPlayer.current
        var founderName by rememberSaveable(obj) { mutableStateOf<String?>(null) }
        val isInCollection = model.collected.contains(obj)
        if (obj.founder != null) {
            LaunchedEffect(obj) {
                founderName = obj.founderId?.let {
                    ProfileCache.getByUuid(it)?.rawName
                }
            }
        }
        Item(
            material = obj.icon ?: Material.PAPER,
            name = component {
                if (obj.alias != null) {
                    text("${obj.alias} ") with mochaYellow without italic()
                    text("(${obj.name})") with mochaSubtext0 without italic()
                } else {
                    text(obj.name) with mochaYellow without italic()
                }
            },
            enchantmentGlint = isInCollection,
            lore = buildList {
                if (isInCollection) {
                    add(component {
                        text("✨ 已收藏") with mochaYellow without italic()
                    })
                }
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
                    text("前往此处") with mochaText without italic()
                })
                add(component {
                    text("右键 ") with mochaLavender without italic()
                    if (!isInCollection) {
                        text("收藏") with mochaText without italic()
                    } else {
                        text("取消收藏") with mochaText without italic()
                    }
                })
            },
            modifier = Modifier.clickable {
                when (clickType) {
                    ClickType.LEFT -> {
                        obj.teleport(player)
                        sync {
                            player.closeInventory()
                        }
                    }

                    ClickType.RIGHT -> {
                        if (WarpManager.getCollection(player).contains(obj)) {
                            WarpManager.removeFromCollection(player, obj)
                            model.collected.remove(obj)
                            if (model.filter == WarpFilter.COLLECTED) {
                                model.contents.remove(obj)
                            }
                        } else {
                            WarpManager.addToCollection(player, obj)
                            model.collected.add(obj)
                        }
                        player.playSound(UI_SUCCEED_SOUND)
                    }

                    else -> {}
                }
            }
        )
    }
}