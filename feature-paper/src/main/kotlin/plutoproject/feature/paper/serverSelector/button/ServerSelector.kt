package plutoproject.feature.paper.serverSelector.button

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.paper.api.menu.dsl.ButtonDescriptor
import plutoproject.feature.paper.serverSelector.screens.ServerSelectorScreen
import plutoproject.framework.common.util.chat.palettes.mochaLavender
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.modifiers.Modifier

val ServerSelectorButtonDescriptor = ButtonDescriptor {
    id = "server_selector:server_selector"
}

@Suppress("FunctionName")
@Composable
fun ServerSelector() {
    val navigator = LocalNavigator.currentOrThrow
    Item(
        material = Material.COMPASS,
        name = component {
            text("选择服务器") with mochaText
        },
        lore = buildList {
            add(component {
                text("踏上新的旅途吧~") with mochaSubtext0
            })
            add(Component.empty())
            add(component {
                text("左键 ") with mochaLavender
                text("选择服务器") with mochaText
            })
        },
        modifier = Modifier.clickable {
            if (clickType != ClickType.LEFT) return@clickable
            navigator.push(ServerSelectorScreen())
        }
    )
}
