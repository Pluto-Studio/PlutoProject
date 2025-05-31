package plutoproject.feature.paper.menu.prebuilt.buttons

import androidx.compose.runtime.Composable
import ink.pmc.advkt.component.*
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.paper.api.menu.dsl.ButtonDescriptor
import plutoproject.framework.common.util.chat.MESSAGE_SOUND
import plutoproject.framework.common.util.chat.palettes.mochaLavender
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.util.coroutine.withSync

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
            text("星社百科") with mochaText without italic()
        },
        lore = buildList {
            add(component {
                text("服务器的百科全书") with mochaSubtext0 without italic()
            })
            add(component {
                text("里面记载了有关星社的一切") with mochaSubtext0 without italic()
            })
            add(Component.empty())
            add(component {
                text("左键 ") with mochaLavender without italic()
                text("获取百科链接") with mochaText without italic()
            })
        },
        modifier = Modifier.clickable {
            if (clickType != ClickType.LEFT) return@clickable
            player.sendMessage(component {
                text("点此打开星社百科") with mochaLavender with underlined() with openUrl("https://wiki.pmc.ink/")
            })
            player.playSound(MESSAGE_SOUND)
            player.withSync {
                player.closeInventory()
            }
        }
    )
}
