package plutoproject.feature.paper.serverSelector.screens

import androidx.compose.runtime.*
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import plutoproject.feature.common.serverSelector.AUTO_JOIN_TOGGLE_PERSIST_KEY
import plutoproject.feature.paper.serverSelector.Ingredient
import plutoproject.feature.paper.serverSelector.Server
import plutoproject.feature.paper.serverSelector.ServerSelectorConfig
import plutoproject.feature.paper.serverSelector.screens.ServerSelectorScreen.AutoJoinState.*
import plutoproject.feature.paper.serverSelector.transferServer
import plutoproject.framework.common.api.databasepersist.DatabasePersist
import plutoproject.framework.common.api.databasepersist.adapters.BooleanTypeAdapter
import plutoproject.framework.common.util.chat.UI_SUCCEED_SOUND
import plutoproject.framework.common.util.chat.palettes.*
import plutoproject.framework.common.util.coroutine.runAsync
import plutoproject.framework.paper.api.interactive.InteractiveScreen
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.animations.spinnerAnimation
import plutoproject.framework.paper.api.interactive.canvas.Menu
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.components.ItemSpacer
import plutoproject.framework.paper.api.interactive.jetpack.Arrangement
import plutoproject.framework.paper.api.interactive.layout.Column
import plutoproject.framework.paper.api.interactive.layout.Row
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.modifiers.fillMaxSize
import plutoproject.framework.paper.api.interactive.modifiers.fillMaxWidth
import plutoproject.framework.paper.api.interactive.modifiers.height
import plutoproject.framework.paper.util.coroutine.withSync

private val titles = arrayOf(
    "开始新的征程！",
    "去往何处？",
    "下一个目标是？",
    "踏上新的旅途吧！",
    "向着诗与远方！"
)

class ServerSelectorScreen : InteractiveScreen(), KoinComponent {
    private val config by inject<ServerSelectorConfig>()

    @Composable
    override fun Content() {
        Menu(
            title = Component.text(titles.random()),
            rows = config.menu.rows,
            bottomBorderAttachment = {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center) {
                    AutoJoin()
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                config.menu.pattern.forEach {
                    PatternLine(it)
                }
            }
        }
    }

    @Suppress("FunctionName")
    @Composable
    private fun PatternLine(pattern: String) {
        Row(modifier = Modifier.fillMaxWidth().height(1)) {
            pattern.forEach { char ->
                if (char.isWhitespace()) {
                    ItemSpacer()
                    return@forEach
                }
                val server = config.servers.firstOrNull { it.menuIcon.first() == char }
                val ingredient = config.menu.ingredients[char.toString()]
                if (ingredient == null || server == null) {
                    ItemSpacer()
                    return@forEach
                }
                Server(server, ingredient)
            }
        }
    }

    @Suppress("FunctionName")
    @Composable
    private fun Server(server: Server, ingredient: Ingredient) {
        val player = LocalPlayer.current
        Item(
            material = ingredient.material,
            name = server.displayName.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE),
            lore = buildList {
                /*
                if (isOnline) {
                    add(component {
                        text("• 在线 ") with mochaGreen
                        text("${bridgeServer?.playerCount} ") with mochaText
                        text("名玩家") with mochaSubtext0
                    })
                } else {
                    add(component {
                        text("× 离线") with mochaMaroon
                    })
                }
                add(Component.empty())
                 */
                addAll(server.description.map {
                    it.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)
                })
                add(Component.empty())
                add(component {
                    text("左键 ") with mochaLavender
                    text("传送至此处") with mochaText
                })
            },
            modifier = Modifier.clickable {
                if (clickType != ClickType.LEFT) return@clickable
                // if (!isOnline) return@clickable
                runAsync { player.transferServer(server.serverId) }
                withSync { player.closeInventory() }
            }
        )
    }

    private enum class AutoJoinState {
        LOADING, ENABLED, DISABLED
    }

    @Suppress("FunctionName")
    @Composable
    private fun AutoJoin() {
        val player = LocalPlayer.current
        var state by remember { mutableStateOf(LOADING) }
        LaunchedEffect(Unit) {
            val container = DatabasePersist.getContainer(player.uniqueId)
            if (!container.getOrDefault(AUTO_JOIN_TOGGLE_PERSIST_KEY, BooleanTypeAdapter, false)) {
                state = DISABLED
                return@LaunchedEffect
            }
            state = ENABLED
        }

        Item(
            material = Material.TRIPWIRE_HOOK,
            name = when (state) {
                LOADING -> Component.text("${spinnerAnimation()} 正在加载...").color(mochaSubtext0)

                ENABLED -> component {
                    text("自动传送 ") with mochaText
                    text("开") with mochaGreen
                }

                DISABLED -> component {
                    text("自动传送 ") with mochaText
                    text("关") with mochaMaroon
                }
            },
            enchantmentGlint = state == ENABLED,
            lore = if (state == LOADING) emptyList() else buildList {
                add(component {
                    text("下次进入时，自动传送上次选择的服务器") with mochaSubtext0
                })
                add(component {
                    text("若需返回此大厅，请使用 ") with mochaSubtext0
                    text("/lobby") with mochaLavender
                })
                add(Component.empty())
                add(component {
                    text("左键 ") with mochaLavender
                    if (state == DISABLED) {
                        text("开启功能") with mochaText
                    } else {
                        text("关闭功能") with mochaText
                    }
                })
            },
            modifier = Modifier.clickable {
                if (clickType != ClickType.LEFT) return@clickable
                if (state == LOADING) return@clickable
                val container = DatabasePersist.getContainer(player.uniqueId)
                if (state == DISABLED) {
                    container.set(AUTO_JOIN_TOGGLE_PERSIST_KEY, BooleanTypeAdapter, true)
                    state = ENABLED
                } else {
                    container.set(AUTO_JOIN_TOGGLE_PERSIST_KEY, BooleanTypeAdapter, false)
                    state = DISABLED
                }
                container.save()
                player.playSound(UI_SUCCEED_SOUND)
            }
        )
    }
}
