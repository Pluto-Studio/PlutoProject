package plutoproject.feature.teleport.paper.buttons

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
import plutoproject.feature.teleport.api.paper.TeleportManager
import plutoproject.feature.teleport.paper.screens.TeleportRequestScreen
import plutoproject.feature.teleport.paper.teleportManager
import plutoproject.foundation.common.text.mochaGreen
import plutoproject.foundation.common.text.mochaLavender
import plutoproject.foundation.common.text.mochaSubtext0
import plutoproject.foundation.common.text.mochaText
import plutoproject.capability.interactive.api.LocalPlayer
import plutoproject.capability.interactive.api.click.clickable
import plutoproject.capability.interactive.api.components.Item
import plutoproject.capability.interactive.api.modifiers.Modifier

val TeleportButtonDescriptor = ButtonDescriptor {
    id = "essentials:teleport"
}

@Composable
@Suppress("FunctionName")
fun Teleport() {
    val player = LocalPlayer.current
    val navigator = LocalNavigator.currentOrThrow
    val hasUnfinishedTpRequest = teleportManager.hasUnfinishedRequest(player)
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
