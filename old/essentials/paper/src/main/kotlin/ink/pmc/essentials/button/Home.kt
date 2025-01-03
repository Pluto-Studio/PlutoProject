package ink.pmc.essentials.button

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.essentials.api.home.Home
import ink.pmc.essentials.api.home.HomeManager
import ink.pmc.essentials.screens.home.HomeListScreen
import ink.pmc.framework.interactive.LocalPlayer
import ink.pmc.framework.interactive.Item
import ink.pmc.framework.interactive.Modifier
import ink.pmc.framework.interactive.click.clickable
import ink.pmc.framework.chat.mochaLavender
import ink.pmc.framework.chat.mochaSubtext0
import ink.pmc.framework.chat.mochaText
import ink.pmc.framework.chat.mochaYellow
import ink.pmc.menu.api.dsl.buttonDescriptor
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType

val HOME_BUTTON_DESCRIPTOR = buttonDescriptor {
    id = "essentials:home"
}

private val homeDesc = component {
    text("为你指明归家路的一盏灯") with mochaSubtext0 without italic()
}


private val homeOpenList = component {
    text("右键 ") with mochaLavender without italic()
    text("打开家列表") with mochaText without italic()
}


private sealed class PreferredHomeState {
    data object Loading : PreferredHomeState()
    class Ready(val home: Home) : PreferredHomeState()
    data object None : PreferredHomeState()
}

@Composable
@Suppress("FunctionName")
fun Home() {
    val player = LocalPlayer.current
    val navigator = LocalNavigator.currentOrThrow
    var preferredHomeState by remember { mutableStateOf<PreferredHomeState>(PreferredHomeState.Loading) }

    LaunchedEffect(Unit) {
        val home = HomeManager.getPreferredHome(player)
        preferredHomeState = if (home != null) PreferredHomeState.Ready(home) else PreferredHomeState.None
    }

    Item(
        material = Material.LANTERN,
        name = component {
            text("明灯") with mochaYellow without italic()
        },
        lore = when (preferredHomeState) {
            is PreferredHomeState.Loading -> buildList {
                add(component {
                    text("正在加载...") with mochaSubtext0 without italic()
                })
            }

            is PreferredHomeState.Ready -> buildList {
                add(homeDesc)
                add(Component.empty())
                add(component {
                    text("左键 ") with mochaLavender without italic()
                    text("传送至首选的家") with mochaText without italic()
                })
                add(homeOpenList)
            }

            is PreferredHomeState.None -> buildList {
                add(homeDesc)
                add(Component.empty())
                add(component {
                    text("你还没有首选的家") with mochaSubtext0 without italic()
                })
                add(component {
                    text("请在编辑家页面中点击「设为首选」") with mochaSubtext0 without italic()
                })
                add(Component.empty())
                add(homeOpenList)
            }
        },
        // enchantmentGlint = state.value > 0,
        modifier = Modifier.clickable {
            when (clickType) {
                ClickType.LEFT -> {
                    val preferred = (preferredHomeState as? PreferredHomeState.Ready)?.home
                    preferred?.teleport(player)
                }

                ClickType.RIGHT -> {
                    navigator.push(HomeListScreen(player))
                }

                else -> {}
            }
        }
    )
}