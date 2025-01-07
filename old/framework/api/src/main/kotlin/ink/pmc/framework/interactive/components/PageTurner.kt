package ink.pmc.framework.interactive.components

import androidx.compose.runtime.Composable
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.framework.interactive.LocalPlayer
import ink.pmc.framework.interactive.Item
import ink.pmc.framework.interactive.Modifier
import ink.pmc.framework.interactive.click.clickable
import ink.pmc.framework.interactive.components.SeparatePageTunerMode.NEXT
import ink.pmc.framework.interactive.components.SeparatePageTunerMode.PREVIOUS
import ink.pmc.framework.chat.UI_PAGING_SOUND
import ink.pmc.framework.chat.mochaLavender
import ink.pmc.framework.chat.mochaText
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType

enum class SeparatePageTunerMode {
    PREVIOUS, NEXT
}

@Suppress("UNUSED", "FunctionName")
@Composable
fun SeparatePageTuner(
    icon: Material = Material.ARROW,
    description: Collection<Component> = emptyList(),
    mode: SeparatePageTunerMode,
    current: Int,
    total: Int,
    turn: suspend () -> Boolean
) {
    val player = LocalPlayer.current
    Item(
        material = icon,
        name = component {
            text("第 $current/$total 页") with mochaText without italic()
        },
        lore = buildList {
            addAll(description)
            add(Component.empty())
            add(component {
                when (mode) {
                    PREVIOUS -> {
                        text("左键 ") with mochaLavender without italic()
                        text("上一页") with mochaText without italic()
                    }

                    NEXT -> {
                        text("左键 ") with mochaLavender without italic()
                        text("下一页") with mochaText without italic()
                    }
                }
            })
        },
        modifier = Modifier.clickable {
            if (clickType != ClickType.LEFT) return@clickable
            if (turn()) player.playSound(UI_PAGING_SOUND)
        }
    )
}