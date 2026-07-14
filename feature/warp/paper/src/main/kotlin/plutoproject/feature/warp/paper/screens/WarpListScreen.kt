package plutoproject.feature.warp.paper.screens

import plutoproject.feature.warp.paper.profileLookup

import plutoproject.feature.warp.paper.warpManager

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.warp.api.paper.Warp
import plutoproject.feature.warp.api.paper.WarpCategory
import plutoproject.feature.warp.api.paper.WarpManager
import plutoproject.capability.profile.api.ProfileLookup
import plutoproject.foundation.common.text.UI_SUCCEED_SOUND
import plutoproject.foundation.common.text.splitLines
import plutoproject.foundation.common.text.*
import plutoproject.foundation.common.time.formatDate
import plutoproject.capability.interactive.api.LocalPlayer
import plutoproject.capability.interactive.api.click.clickable
import plutoproject.capability.interactive.api.components.Item
import plutoproject.capability.interactive.api.layout.list.FilterListMenu
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.feature.warp.paper.timezone
import plutoproject.feature.warp.paper.aliasOrName
import plutoproject.feature.warp.paper.coroutineContext
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
                    profileLookup.lookupByUuid(it)?.name
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
                        if (warpManager.getCollection(player).contains(obj)) {
                            warpManager.removeFromCollection(player, obj)
                            model.collected.remove(obj)
                            if (model.filter == WarpFilter.COLLECTED) {
                                model.contents.remove(obj)
                            }
                        } else {
                            warpManager.addToCollection(player, obj)
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
