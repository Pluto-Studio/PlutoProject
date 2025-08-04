package plutoproject.feature.paper.warp.screens

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.paper.api.warp.Warp
import plutoproject.feature.paper.api.warp.WarpCategory
import plutoproject.feature.paper.api.warp.WarpManager
import plutoproject.framework.common.api.profile.ProfileLookup
import plutoproject.framework.common.util.chat.UI_SUCCEED_SOUND
import plutoproject.framework.common.util.chat.component.splitLines
import plutoproject.framework.common.util.chat.palettes.*
import plutoproject.framework.common.util.time.formatDate
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.layout.list.FilterListMenu
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.provider.timezone
import plutoproject.framework.paper.api.worldalias.aliasOrName
import plutoproject.framework.paper.util.coroutine.coroutineContext
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
                    ProfileLookup.lookupByUuid(it)?.name
                }
            }
        }
        Item(
            material = obj.icon ?: Material.PAPER,
            name = component {
                if (obj.alias != null) {
                    text("${obj.alias} ") with mochaYellow
                    text("(${obj.name})") with mochaSubtext0
                } else {
                    text(obj.name) with mochaYellow
                }
            },
            enchantmentGlint = isInCollection,
            lore = buildList {
                if (isInCollection) {
                    add(component {
                        text("✨ 已收藏") with mochaYellow
                    })
                }
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
                    text("前往此处") with mochaText
                })
                add(component {
                    text("右键 ") with mochaLavender
                    if (!isInCollection) {
                        text("收藏") with mochaText
                    } else {
                        text("取消收藏") with mochaText
                    }
                })
            },
            modifier = Modifier.clickable {
                when (clickType) {
                    ClickType.LEFT -> {
                        obj.teleport(player)
                        withContext(player.coroutineContext) {
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
