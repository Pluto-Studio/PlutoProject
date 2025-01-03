package ink.pmc.essentials.button

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.essentials.screens.warp.WarpListScreen
import ink.pmc.framework.interactive.Item
import ink.pmc.framework.interactive.Modifier
import ink.pmc.framework.interactive.click.clickable
import ink.pmc.framework.chat.mochaLavender
import ink.pmc.framework.chat.mochaSapphire
import ink.pmc.framework.chat.mochaSubtext0
import ink.pmc.framework.chat.mochaText
import ink.pmc.menu.api.dsl.buttonDescriptor
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType

val WARP_BUTTON_DESCRIPTOR = buttonDescriptor {
    id = "essentials:warp"
}

@Composable
@Suppress("FunctionName")
fun Warp() {
    val navigator = LocalNavigator.currentOrThrow
    Item(
        material = Material.MINECART,
        name = component {
            text("巡回列车") with mochaSapphire without italic()
        },
        lore = buildList {
            add(component {
                text("参观其他玩家的机械、建筑与城镇") with mochaSubtext0 without italic()
            })
            add(Component.empty())
            add(component {
                text("左键 ") with mochaLavender without italic()
                text("打开地标列表") with mochaText without italic()
            })
        },
        modifier = Modifier.clickable {
            if (clickType != ClickType.LEFT) return@clickable
            navigator.push(WarpListScreen())
        }
    )
}