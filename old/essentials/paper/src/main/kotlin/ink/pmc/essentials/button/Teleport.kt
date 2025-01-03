package ink.pmc.essentials.button

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.essentials.api.teleport.TeleportManager
import ink.pmc.essentials.screens.teleport.TeleportRequestScreen
import ink.pmc.framework.interactive.LocalPlayer
import ink.pmc.framework.interactive.Item
import ink.pmc.framework.interactive.Modifier
import ink.pmc.framework.interactive.click.clickable
import ink.pmc.framework.chat.mochaGreen
import ink.pmc.framework.chat.mochaLavender
import ink.pmc.framework.chat.mochaSubtext0
import ink.pmc.framework.chat.mochaText
import ink.pmc.menu.api.dsl.buttonDescriptor
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType

val TELEPORT_BUTTON_DESCRIPTOR = buttonDescriptor {
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
            text("定向传送") with mochaGreen without italic()
        },
        lore = if (!hasUnfinishedTpRequest) buildList {
            add(component {
                text("拜访世界中的其他玩家") with mochaSubtext0 without italic()
            })
            add(Component.empty())
            add(component {
                text("左键 ") with mochaLavender without italic()
                text("发起传送请求") with mochaText without italic()
            })
        } else buildList {
            add(component {
                text("你还有未完成的传送请求") with mochaSubtext0 without italic()
            })
            add(component {
                text("可使用 ") with mochaSubtext0 without italic()
                text("/tpcancel ") with mochaLavender without italic()
                text("来取消") with mochaSubtext0 without italic()
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