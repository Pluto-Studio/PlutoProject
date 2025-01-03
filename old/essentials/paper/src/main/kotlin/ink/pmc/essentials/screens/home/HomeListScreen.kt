package ink.pmc.essentials.screens.home

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.essentials.api.home.Home
import ink.pmc.framework.chat.*
import ink.pmc.framework.interactive.click.clickable
import ink.pmc.framework.interactive.jetpack.Arrangement
import ink.pmc.framework.interactive.layout.Row
import ink.pmc.framework.interactive.layout.list.ListMenu
import ink.pmc.framework.concurrent.sync
import ink.pmc.framework.interactive.*
import ink.pmc.framework.time.formatDate
import ink.pmc.framework.time.zoneId
import ink.pmc.framework.world.aliasOrName
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.event.inventory.ClickType
import java.time.ZonedDateTime

class HomeListScreen(private val viewing: OfflinePlayer) : ListMenu<Home, HomeListScreenModel>() {
    @Composable
    override fun MenuLayout() {
        LocalListMenuOptions.current.title = Component.text("家")
        super.MenuLayout()
    }

    @Composable
    override fun BottomBorderAttachment() {
        if (LocalListMenuModel.current.isLoading) return
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
            PreviousTurner()
            Create()
            NextTurner()
        }
    }

    @Composable
    @Suppress("FunctionName")
    private fun Create() {
        val player = LocalPlayer.current
        val navigator = LocalNavigator.currentOrThrow
        if (player != viewing) {
            Spacer(modifier = Modifier.height(1).width(1))
            return
        }
        Item(
            material = Material.OAK_SIGN,
            name = component {
                text("创建家") with mochaText without italic()
            },
            lore = listOf(
                Component.empty(),
                component {
                    text("左键 ") with mochaLavender without italic()
                    text("在当前位置创建家") with mochaText without italic()
                }
            ),
            modifier = Modifier.clickable {
                if (clickType != ClickType.LEFT) return@clickable
                navigator.push(HomeCreatorScreen())
            }
        )
    }

    @Composable
    override fun modelProvider(): HomeListScreenModel {
        return HomeListScreenModel(viewing)
    }

    @Composable
    override fun Element(obj: Home) {
        val player = LocalPlayer.current
        val navigator = LocalNavigator.currentOrThrow
        Item(
            material = when {
                obj.icon != null -> obj.icon!!
                obj.isPreferred -> Material.SUNFLOWER
                else -> Material.PAPER
            },
            name = component {
                text(obj.name) with mochaYellow without italic()
            },
            lore = buildList {
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
                if (obj.isPreferred) add(component {
                    text("√ 首选的家") with mochaGreen without italic()
                })
                if (obj.isStarred) add(component {
                    text("✨ 收藏的家") with mochaYellow without italic()
                })
                add(Component.empty())
                add(component {
                    text("左键 ") with mochaLavender without italic()
                    text("传送到该位置") with mochaText without italic()
                })
                if (player != viewing) return@buildList
                add(component {
                    text("右键 ") with mochaLavender without italic()
                    text("编辑家") with mochaText without italic()
                })
            },
            enchantmentGlint = obj.isPreferred || obj.isStarred,
            modifier = Modifier.clickable {
                when (clickType) {
                    ClickType.LEFT -> {
                        obj.teleport(player)
                        sync {
                            player.closeInventory()
                        }
                    }

                    ClickType.RIGHT -> {
                        if (player != viewing) return@clickable
                        navigator.push(HomeEditorScreen(obj))
                    }

                    else -> {}
                }
            }
        )
    }
}