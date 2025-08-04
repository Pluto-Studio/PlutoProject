package plutoproject.feature.paper.home.screens

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.paper.api.home.Home
import plutoproject.framework.common.util.chat.palettes.*
import plutoproject.framework.common.util.time.formatDate
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.components.Spacer
import plutoproject.framework.paper.api.interactive.jetpack.Arrangement
import plutoproject.framework.paper.api.interactive.layout.Row
import plutoproject.framework.paper.api.interactive.layout.list.ListMenu
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.modifiers.fillMaxSize
import plutoproject.framework.paper.api.interactive.modifiers.height
import plutoproject.framework.paper.api.interactive.modifiers.width
import plutoproject.framework.paper.api.provider.timezone
import plutoproject.framework.paper.api.worldalias.aliasOrName
import plutoproject.framework.paper.util.coroutine.coroutineContext
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
                text("创建家") with mochaText
            },
            lore = listOf(
                Component.empty(),
                component {
                    text("左键 ") with mochaLavender
                    text("在当前位置创建家") with mochaText
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
                text(obj.name) with mochaYellow
            },
            lore = buildList {
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
                if (obj.isPreferred) add(component {
                    text("√ 首选的家") with mochaGreen
                })
                if (obj.isStarred) add(component {
                    text("✨ 收藏的家") with mochaYellow
                })
                add(Component.empty())
                add(component {
                    text("左键 ") with mochaLavender
                    text("传送到该位置") with mochaText
                })
                if (player != viewing) return@buildList
                add(component {
                    text("右键 ") with mochaLavender
                    text("编辑家") with mochaText
                })
            },
            enchantmentGlint = obj.isPreferred || obj.isStarred,
            modifier = Modifier.clickable {
                when (clickType) {
                    ClickType.LEFT -> {
                        obj.teleport(player)
                        withContext(player.coroutineContext) {
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
