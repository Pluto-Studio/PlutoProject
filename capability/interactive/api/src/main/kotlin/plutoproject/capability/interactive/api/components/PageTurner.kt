package plutoproject.capability.interactive.api.components

import androidx.compose.runtime.Composable
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.foundation.common.text.UI_PAGING_SOUND
import plutoproject.foundation.common.text.mochaLavender
import plutoproject.foundation.common.text.mochaText
import plutoproject.capability.interactive.api.LocalPlayer
import plutoproject.capability.interactive.api.click.clickable
import plutoproject.capability.interactive.api.components.SeparatePageTunerMode.NEXT
import plutoproject.capability.interactive.api.components.SeparatePageTunerMode.PREVIOUS
import plutoproject.capability.interactive.api.modifiers.Modifier

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
            text("第 $current/$total 页") with mochaText
        },
        lore = buildList {
            addAll(description)
            add(Component.empty())
            add(component {
                when (mode) {
                    PREVIOUS -> {
                        text("左键 ") with mochaLavender
                        text("上一页") with mochaText
                    }

                    NEXT -> {
                        text("左键 ") with mochaLavender
                        text("下一页") with mochaText
                    }
                }
            })
        },
        modifier = Modifier.clickable {
            if (clickType != ClickType.LEFT) {
                return@clickable
            }
            if (turn()) player.playSound(UI_PAGING_SOUND)
        }
    )
}
