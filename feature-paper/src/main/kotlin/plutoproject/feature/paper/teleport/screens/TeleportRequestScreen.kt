package plutoproject.feature.paper.teleport.screens

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.replace
import ink.pmc.advkt.component.text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.meta.SkullMeta
import plutoproject.feature.paper.api.teleport.TeleportDirection
import plutoproject.feature.paper.api.teleport.TeleportManager
import plutoproject.feature.paper.teleport.COMMAND_TPAHERE_SUCCEED
import plutoproject.feature.paper.teleport.COMMAND_TPA_SUCCEED
import plutoproject.framework.common.util.chat.UI_SUCCEED_SOUND
import plutoproject.framework.common.util.chat.palettes.*
import plutoproject.framework.common.util.time.toFormattedComponent
import plutoproject.framework.common.util.time.ticks
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.components.ItemSpacer
import plutoproject.framework.paper.api.interactive.layout.list.ListMenu
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.worldalias.aliasOrName
import plutoproject.framework.paper.util.coroutine.withSync
import plutoproject.framework.paper.util.dsl.ItemStack
import kotlin.time.Duration.Companion.seconds

class TeleportRequestScreen : ListMenu<Player, TeleportRequestScreenModel>() {
    @Composable
    override fun modelProvider(): TeleportRequestScreenModel {
        val player = LocalPlayer.current
        return TeleportRequestScreenModel(player)
    }

    @Composable
    override fun MenuLayout() {
        LocalListMenuOptions.current.title = Component.text("选择玩家")
        val model = LocalListMenuModel.current
        LaunchedEffect(model.onlinePlayers.size) {
            model.loadPageContents()
        }
        super.MenuLayout()
    }

    @Composable
    override fun Element(obj: Player) {
        val model = LocalListMenuModel.current
        val options = LocalListMenuOptions.current
        val coroutineScope = rememberCoroutineScope()
        val player = LocalPlayer.current
        val navigator = LocalNavigator.currentOrThrow

        var world by remember(obj) { mutableStateOf(obj.location.world.aliasOrName) }
        var x by remember(obj) { mutableStateOf(obj.location.blockX) }
        var y by remember(obj) { mutableStateOf(obj.location.blockY) }
        var z by remember(obj) { mutableStateOf(obj.location.blockZ) }

        LaunchedEffect(obj) {
            while (true) {
                world = obj.location.world.aliasOrName
                x = obj.location.blockX
                y = obj.location.blockY
                z = obj.location.blockZ
                delay(5.ticks)
            }
        }

        if (model.isRequestSent && model.requestSentTo != obj) {
            ItemSpacer()
            return
        }

        Item(
            itemStack = ItemStack(Material.PLAYER_HEAD) {
                displayName = if (model.isRequestSent) component {
                    text("√ 已发送") with mochaGreen
                } else component {
                    text(obj.name) with mochaFlamingo
                }
                lore(
                    if (model.isRequestSent) {
                        emptyList()
                    } else buildList {
                        add(component {
                            text("$world $x, $y, $z") with mochaSubtext0
                        })
                        add(Component.empty())
                        add(component {
                            text("左键 ") with mochaLavender
                            text("请求传送至其位置") with mochaText
                        })
                        add(component {
                            text("右键 ") with mochaLavender
                            text("请求其传送至你这里") with mochaText
                        })
                    }
                )
                meta {
                    this as SkullMeta
                    playerProfile = obj.playerProfile
                    setEnchantmentGlintOverride(model.requestSentTo == obj)
                }
            },
            modifier = Modifier.clickable {
                if (model.isRequestSent || model.requestSentTo != null) return@clickable
                if (TeleportManager.hasUnfinishedRequest(player)) return@clickable
                val direction = when (clickType) {
                    ClickType.LEFT -> TeleportDirection.GO
                    ClickType.RIGHT -> TeleportDirection.COME
                    else -> return@clickable
                }
                val message = when (direction) {
                    TeleportDirection.GO -> COMMAND_TPA_SUCCEED
                    TeleportDirection.COME -> COMMAND_TPAHERE_SUCCEED
                }
                TeleportManager.createRequest(player, obj, direction)
                model.isRequestSent = true
                model.requestSentTo = obj
                options.centerBackground = true
                coroutineScope.launch {
                    delay(1.seconds)
                    if (!navigator.pop()) withSync {
                        player.closeInventory()
                    }
                }
                player.playSound(UI_SUCCEED_SOUND)
                player.sendMessage(
                    message
                        .replace("<player>", obj.name)
                        .replace("<expire>", TeleportManager.defaultRequestOptions.expireAfter.toFormattedComponent())
                )
            }
        )
    }
}
