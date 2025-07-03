package plutoproject.feature.paper.warp.buttons

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
import plutoproject.feature.paper.warp.screens.WarpListScreen
import plutoproject.framework.common.util.chat.palettes.mochaLavender
import plutoproject.framework.common.util.chat.palettes.mochaSapphire
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.modifiers.Modifier

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
