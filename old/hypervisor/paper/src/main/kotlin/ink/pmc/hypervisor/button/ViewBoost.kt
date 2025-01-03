package ink.pmc.hypervisor.button

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.framework.chat.*
import ink.pmc.framework.interactive.Item
import ink.pmc.framework.interactive.LocalPlayer
import ink.pmc.framework.interactive.Modifier
import ink.pmc.framework.interactive.click.clickable
import ink.pmc.hypervisor.DynamicScheduling
import ink.pmc.hypervisor.DynamicViewDistanceState.*
import ink.pmc.menu.api.dsl.buttonDescriptor
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType

val VIEW_BOOST_BUTTON_DESCRIPTOR = buttonDescriptor {
    id = "hypervisor:view_boost"
}

private val disabled = component {
    text("关") with mochaMaroon without italic()
}

private val enabled = component {
    text("开") with mochaGreen without italic()
}

private val viewBoost = component {
    text("视距拓展") with mochaText without italic()
}

private val viewBoostDesc = listOf(
    component {
        text("可让服务器为你发送至多 ") with mochaSubtext0 without italic()
        text("16 ") with mochaText without italic()
        text("视距") with mochaSubtext0 without italic()
    },
    component {
        text("以提升观景体验") with mochaSubtext0 without italic()
    }
)

private val viewBoostDisabledDuePing = buildList {
    addAll(viewBoostDesc)
    add(Component.empty())
    add(component {
        text("此功能仅在延迟小于 ") with mochaYellow without italic()
        text("100ms ") with mochaText without italic()
        text("时可用") with mochaYellow without italic()
    })
    add(component {
        text("可尝试切换到一个质量更好的网络接入点") with mochaYellow without italic()
    })
}

@Composable
@Suppress("FunctionName")
fun ViewBoost() {
    val player = LocalPlayer.current
    var state by mutableStateOf(DynamicScheduling.getViewDistanceLocally(player))
    Item(
        material = Material.SPYGLASS,
        name = when (state) {
            ENABLED -> viewBoost
                .append(Component.text(" "))
                .append(enabled)

            DISABLED -> viewBoost
                .append(Component.text(" "))
                .append(disabled)

            DISABLED_DUE_PING -> viewBoost
                .append(Component.text(" "))
                .append(disabled)

            ENABLED_BUT_DISABLED_DUE_PING -> viewBoost
                .append(Component.text(" "))
                .append(disabled)

            DISABLED_DUE_VHOST -> viewBoost
                .append(Component.text(" "))
                .append(disabled)
        },
        enchantmentGlint = state == ENABLED,
        lore = when (state) {
            ENABLED -> buildList {
                addAll(viewBoostDesc)
                add(Component.empty())
                add(component {
                    text("将渲染距离调至 ") with mochaSubtext0 without italic()
                    text("16 ") with mochaText without italic()
                    text("或更高") with mochaSubtext0 without italic()
                })
                add(component {
                    text("以使此功能生效") with mochaSubtext0 without italic()
                })
                add(Component.empty())
                add(component {
                    text("左键 ") with mochaLavender without italic()
                    text("关闭功能") with mochaText without italic()
                })
            }

            DISABLED -> buildList {
                addAll(viewBoostDesc)
                add(Component.empty())
                add(component {
                    text("左键 ") with mochaLavender without italic()
                    text("开启功能") with mochaText without italic()
                })
            }

            DISABLED_DUE_PING -> viewBoostDisabledDuePing
            ENABLED_BUT_DISABLED_DUE_PING -> viewBoostDisabledDuePing
            DISABLED_DUE_VHOST -> buildList {
                addAll(viewBoostDesc)
                add(Component.empty())
                add(component {
                    text("你正在使用的连接线路不支持此功能") with mochaYellow without italic()
                })
                add(component {
                    text("请切换至主线路") with mochaYellow without italic()
                })
            }
        },
        modifier = Modifier.clickable {
            if (clickType != ClickType.LEFT) return@clickable
            when (state) {
                ENABLED -> {
                    DynamicScheduling.setViewDistance(player, false)
                    player.playSound(UI_SUCCEED_SOUND)
                    state = DISABLED
                }

                DISABLED -> {
                    DynamicScheduling.setViewDistance(player, true)
                    player.playSound(UI_SUCCEED_SOUND)
                    state = ENABLED
                }

                DISABLED_DUE_PING -> return@clickable
                DISABLED_DUE_VHOST -> return@clickable
                ENABLED_BUT_DISABLED_DUE_PING -> return@clickable
            }
        }
    )
}