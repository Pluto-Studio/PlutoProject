package plutoproject.feature.paper.menu.prebuilt.buttons

import androidx.compose.runtime.*
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.paper.api.menu.dsl.ButtonDescriptor
import plutoproject.feature.paper.menu.hooks.CO_NEAR_COMMAND
import plutoproject.feature.paper.menu.hooks.isCoreProtectAvailable
import plutoproject.feature.paper.menu.hooks.isInspecting
import plutoproject.framework.common.util.chat.UI_SUCCEED_SOUND
import plutoproject.framework.common.util.chat.palettes.*
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.components.NotAvailable
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.util.coroutine.withSync

val InspectButtonDescriptor = ButtonDescriptor {
    id = "menu:inspect"
}

@Composable
@Suppress("FunctionName")
fun Inspect() {
    val player = LocalPlayer.current
    var isInspecting by remember { mutableStateOf(player.isInspecting) }
    if (!isCoreProtectAvailable) {
        NotAvailable(
            material = Material.ENDER_EYE,
            name = component {
                text("观察模式") with mochaText without italic()
            }
        )
        return
    }
    Item(
        material = Material.ENDER_EYE,
        name = if (!isInspecting) component {
            text("观察模式 ") with mochaText without italic()
            text("关") with mochaMaroon without italic()
        } else component {
            text("观察模式 ") with mochaText without italic()
            text("开") with mochaGreen without italic()
        },
        lore = if (!isInspecting) buildList {
            add(component {
                text("将周围的变化一览无余") with mochaSubtext0 without italic()
            })
            add(Component.empty())
            add(component {
                text("左键 ") with mochaLavender without italic()
                text("开启观察模式") with mochaText without italic()
            })
            add(component {
                text("右键 ") with mochaLavender without italic()
                text("观察四周变化") with mochaText without italic()
            })
        } else buildList {
            add(component {
                text("将周围的变化一览无余") with mochaSubtext0 without italic()
            })
            add(Component.empty())
            add(component {
                text("观察模式已开启") with mochaSubtext0 without italic()
            })
            add(component {
                text("使用左键或右键点击来观察变化") with mochaSubtext0 without italic()
            })
            add(Component.empty())
            add(component {
                text("左键 ") with mochaLavender without italic()
                text("关闭观察模式") with mochaText without italic()
            })
        },
        enchantmentGlint = isInspecting,
        modifier = Modifier.clickable {
            when (clickType) {
                ClickType.LEFT -> {
                    player.isInspecting = !isInspecting
                    isInspecting = !isInspecting
                    player.playSound(UI_SUCCEED_SOUND)
                    return@clickable
                }

                ClickType.RIGHT -> {
                    player.playSound(UI_SUCCEED_SOUND)
                    player.withSync {
                        player.performCommand(CO_NEAR_COMMAND)
                        player.closeInventory()
                    }
                }

                else -> {}
            }
        }
    )
}
