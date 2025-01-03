package ink.pmc.serverselector.button

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.framework.interactive.Item
import ink.pmc.framework.interactive.Modifier
import ink.pmc.framework.interactive.click.clickable
import ink.pmc.framework.chat.mochaLavender
import ink.pmc.framework.chat.mochaSubtext0
import ink.pmc.framework.chat.mochaText
import ink.pmc.menu.api.dsl.buttonDescriptor
import ink.pmc.serverselector.screen.ServerSelectorScreen
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType

val SERVER_SELECTOR_BUTTON_DESCRIPTOR = buttonDescriptor {
    id = "server_selector:server_selector"
}

@Suppress("FunctionName")
@Composable
fun ServerSelector() {
    val navigator = LocalNavigator.currentOrThrow
    Item(
        material = Material.COMPASS,
        name = component {
            text("选择服务器") with mochaText without italic()
        },
        lore = buildList {
            add(component {
                text("踏上新的旅途吧~") with mochaSubtext0 without italic()
            })
            add(Component.empty())
            add(component {
                text("左键 ") with mochaLavender without italic()
                text("选择服务器") with mochaText without italic()
            })
        },
        modifier = Modifier.clickable {
            if (clickType != ClickType.LEFT) return@clickable
            navigator.push(ServerSelectorScreen())
        }
    )
}