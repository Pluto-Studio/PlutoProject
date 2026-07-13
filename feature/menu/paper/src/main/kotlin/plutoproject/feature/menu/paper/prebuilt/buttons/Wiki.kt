package plutoproject.feature.menu.paper.prebuilt.buttons

import androidx.compose.runtime.Composable
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.openUrl
import ink.pmc.advkt.component.text
import ink.pmc.advkt.component.underlined
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.menu.api.paper.dsl.ButtonDescriptor
import plutoproject.foundation.common.text.MESSAGE_SOUND
import plutoproject.foundation.common.text.mochaLavender
import plutoproject.foundation.common.text.mochaSubtext0
import plutoproject.foundation.common.text.mochaText
import plutoproject.capability.interactive.api.LocalPlayer
import plutoproject.capability.interactive.api.click.clickable
import plutoproject.capability.interactive.api.components.Item
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.feature.menu.paper.coroutineContext

val WikiButtonDescriptor = ButtonDescriptor {
    id = "menu:wiki"
}

@Composable
@Suppress("FunctionName")
fun Wiki() {
    val player = LocalPlayer.current
    Item(
        material = Material.BOOK,
        name = component {
            text("星社百科") with mochaText
        },
        lore = buildList {
            add(component {
                text("服务器的百科全书") with mochaSubtext0
            })
            add(component {
                text("里面记载了有关星社的一切") with mochaSubtext0
            })
            add(Component.empty())
            add(component {
                text("左键 ") with mochaLavender
                text("获取百科链接") with mochaText
            })
        },
        modifier = Modifier.clickable {
            if (clickType != ClickType.LEFT) return@clickable
            player.sendMessage(component {
                text("点此打开星社百科") with mochaLavender with underlined() with openUrl("https://wiki.pmc.ink/")
            })
            player.playSound(MESSAGE_SOUND)
            withContext(player.coroutineContext) {
                player.closeInventory()
            }
        }
    )
}
