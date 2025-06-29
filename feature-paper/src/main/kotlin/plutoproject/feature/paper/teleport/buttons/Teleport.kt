package plutoproject.feature.paper.teleport.buttons

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
import plutoproject.feature.paper.api.teleport.TeleportManager
import plutoproject.feature.paper.teleport.screens.TeleportRequestScreen
import plutoproject.framework.common.util.chat.palettes.mochaGreen
import plutoproject.framework.common.util.chat.palettes.mochaLavender
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.modifiers.Modifier

val TeleportButtonDescriptor = ButtonDescriptor {
    id = "essentials:teleport"
}

@Composable
@Suppress("FunctionName")
fun Teleport() {
    val player = LocalPlayer.current
    val navigator = LocalNavigator.currentOrThrow
    val hasUnfinishedTpRequest = TeleportManager.hasUnfinishedRequest(player)
    Item(
        material = Material.ENDER_PEARL,
        name = component {
            text("定向传送") with mochaGreen
        },
        lore = if (!hasUnfinishedTpRequest) buildList {
            add(component {
                text("拜访世界中的其他玩家") with mochaSubtext0
            })
            add(Component.empty())
            add(component {
                text("左键 ") with mochaLavender
                text("发起传送请求") with mochaText
            })
        } else buildList {
            add(component {
                text("你还有未完成的传送请求") with mochaSubtext0
            })
            add(component {
                text("可使用 ") with mochaSubtext0
                text("/tpcancel ") with mochaLavender
                text("来取消") with mochaSubtext0
            })
        },
        enchantmentGlint = hasUnfinishedTpRequest,
        modifier = Modifier.clickable {
            if (hasUnfinishedTpRequest) return@clickable
            if (clickType != ClickType.LEFT) return@clickable
            navigator.push(TeleportRequestScreen())
        }
    )
}
