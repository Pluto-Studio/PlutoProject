package plutoproject.framework.paper.api.interactive.components

import androidx.compose.runtime.*
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.framework.common.util.chat.UI_SELECT_SOUND
import plutoproject.framework.common.util.chat.palettes.mochaLavender
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.common.util.chat.palettes.mochaYellow
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.modifiers.Modifier

@Suppress("UNUSED", "FunctionName")
@Composable
fun Selector(
    title: Component,
    icon: Material = Material.STICK,
    description: List<Component> = emptyList(),
    options: List<String>,
    default: Int = 0,
    highlightColor: TextColor = mochaYellow,
    goNext: suspend () -> Unit,
    goPrevious: suspend () -> Unit
) {
    require(options.isNotEmpty()) { "Options cannot be empty" }
    val player = LocalPlayer.current
    var current by remember { mutableStateOf(default) }
    Item(
        name = title,
        material = icon,
        lore = buildList {
            addAll(description)
            addAll(options.mapIndexed { index, s ->
                component {
                    text("» $s") with (if (current == index) highlightColor else mochaSubtext0)
                }
            })
            add(Component.empty())
            add(component {
                text("左键 ") with mochaLavender
                text("向后切换") with mochaText
            })
            add(component {
                text("右键 ") with mochaLavender
                text("向前切换") with mochaText
            })
        },
        modifier = Modifier.clickable {
            when (clickType) {
                ClickType.LEFT -> {
                    goNext()
                    current = if (current == options.lastIndex) 0 else current + 1
                    player.playSound(UI_SELECT_SOUND)
                }

                ClickType.RIGHT -> {
                    goPrevious()
                    current = if (current == 0) options.lastIndex else current - 1
                    player.playSound(UI_SELECT_SOUND)
                }

                else -> {}
            }
        }
    )
}
