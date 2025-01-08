package plutoproject.feature.paper.dynamicScheduler.buttons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.paper.api.dynamicScheduler.DynamicScheduler
import plutoproject.feature.paper.api.dynamicScheduler.DynamicViewDistanceState
import plutoproject.feature.paper.api.menu.dsl.ButtonDescriptor
import plutoproject.framework.common.util.chat.SoundConstants
import plutoproject.framework.common.util.chat.palettes.*
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.modifiers.Modifier

val ViewBoostButtonDescriptor = ButtonDescriptor {
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
    var state by mutableStateOf(DynamicScheduler.getViewDistanceLocally(player))
    Item(
        material = Material.SPYGLASS,
        name = when (state) {
            DynamicViewDistanceState.ENABLED -> viewBoost
                .append(Component.text(" "))
                .append(enabled)

            DynamicViewDistanceState.DISABLED -> viewBoost
                .append(Component.text(" "))
                .append(disabled)

            DynamicViewDistanceState.DISABLED_DUE_PING -> viewBoost
                .append(Component.text(" "))
                .append(disabled)

            DynamicViewDistanceState.ENABLED_BUT_DISABLED_DUE_PING -> viewBoost
                .append(Component.text(" "))
                .append(disabled)

            DynamicViewDistanceState.DISABLED_DUE_VHOST -> viewBoost
                .append(Component.text(" "))
                .append(disabled)
        },
        enchantmentGlint = state == DynamicViewDistanceState.ENABLED,
        lore = when (state) {
            DynamicViewDistanceState.ENABLED -> buildList {
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

            DynamicViewDistanceState.DISABLED -> buildList {
                addAll(viewBoostDesc)
                add(Component.empty())
                add(component {
                    text("左键 ") with mochaLavender without italic()
                    text("开启功能") with mochaText without italic()
                })
            }

            DynamicViewDistanceState.DISABLED_DUE_PING -> viewBoostDisabledDuePing
            DynamicViewDistanceState.ENABLED_BUT_DISABLED_DUE_PING -> viewBoostDisabledDuePing
            DynamicViewDistanceState.DISABLED_DUE_VHOST -> buildList {
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
                DynamicViewDistanceState.ENABLED -> {
                    DynamicScheduler.setViewDistance(player, false)
                    player.playSound(SoundConstants.UI.succeed)
                    state = DynamicViewDistanceState.DISABLED
                }

                DynamicViewDistanceState.DISABLED -> {
                    DynamicScheduler.setViewDistance(player, true)
                    player.playSound(SoundConstants.UI.succeed)
                    state = DynamicViewDistanceState.ENABLED
                }

                DynamicViewDistanceState.DISABLED_DUE_PING -> return@clickable
                DynamicViewDistanceState.DISABLED_DUE_VHOST -> return@clickable
                DynamicViewDistanceState.ENABLED_BUT_DISABLED_DUE_PING -> return@clickable
            }
        }
    )
}
