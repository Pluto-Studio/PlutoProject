package plutoproject.feature.home.paper

import plutoproject.feature.home.paper.homeManager

import androidx.compose.runtime.*
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.home.api.paper.Home
import plutoproject.feature.home.api.paper.HomeManager
import plutoproject.feature.menu.api.paper.dsl.ButtonDescriptor
import plutoproject.feature.home.paper.screens.HomeListScreen
import plutoproject.foundation.common.text.mochaLavender
import plutoproject.foundation.common.text.mochaSubtext0
import plutoproject.foundation.common.text.mochaText
import plutoproject.foundation.common.text.mochaYellow
import plutoproject.capability.interactive.api.LocalPlayer
import plutoproject.capability.interactive.api.animations.spinnerAnimation
import plutoproject.capability.interactive.api.click.clickable
import plutoproject.capability.interactive.api.components.Item
import plutoproject.capability.interactive.api.modifiers.Modifier

val HomeButtonDescriptor = ButtonDescriptor {
    id = "essentials:home"
}

private val homeDesc = component {
    text("为你指明归家路的一盏灯") with mochaSubtext0
}


private val homeOpenList = component {
    text("右键 ") with mochaLavender
    text("打开家列表") with mochaText
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
        val home = homeManager.getPreferredHome(player)
        preferredHomeState = if (home != null) PreferredHomeState.Ready(home) else PreferredHomeState.None
    }

    Item(
        material = Material.LANTERN,
        name = component {
            text("明灯") with mochaYellow
        },
        lore = when (preferredHomeState) {
            is PreferredHomeState.Loading -> buildList {
                add(Component.text("${spinnerAnimation()} 正在加载...").color(mochaSubtext0))
            }

            is PreferredHomeState.Ready -> buildList {
                add(homeDesc)
                add(Component.empty())
                add(component {
                    text("左键 ") with mochaLavender
                    text("传送至首选的家") with mochaText
                })
                add(homeOpenList)
            }

            is PreferredHomeState.None -> buildList {
                add(homeDesc)
                add(Component.empty())
                add(component {
                    text("你还没有首选的家") with mochaSubtext0
                })
                add(component {
                    text("请在编辑家页面中点击「设为首选」") with mochaSubtext0
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
