package plutoproject.feature.warp.paper.buttons

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.menu.api.paper.dsl.ButtonDescriptor
import plutoproject.feature.warp.paper.screens.WarpListScreen
import plutoproject.foundation.common.text.mochaLavender
import plutoproject.foundation.common.text.mochaSapphire
import plutoproject.foundation.common.text.mochaSubtext0
import plutoproject.foundation.common.text.mochaText
import plutoproject.capability.interactive.api.click.clickable
import plutoproject.capability.interactive.api.components.Item
import plutoproject.capability.interactive.api.modifiers.Modifier

val WarpButtonDescriptor = ButtonDescriptor {
    id = "essentials:warp"
}

@Composable
@Suppress("FunctionName")
fun Warp() {
    val navigator = LocalNavigator.currentOrThrow
    Item(
        material = Material.MINECART,
        name = component {
            text("巡回列车") with mochaSapphire
        },
        lore = buildList {
            add(component {
                text("参观其他玩家的机械、建筑与城镇") with mochaSubtext0
            })
            add(Component.empty())
            add(component {
                text("左键 ") with mochaLavender
                text("打开地标列表") with mochaText
            })
        },
        modifier = Modifier.clickable {
            if (clickType != ClickType.LEFT) return@clickable
            navigator.push(WarpListScreen())
        }
    )
}
