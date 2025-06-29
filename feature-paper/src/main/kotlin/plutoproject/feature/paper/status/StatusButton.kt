package plutoproject.feature.paper.status

import androidx.compose.runtime.*
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.paper.api.menu.dsl.ButtonDescriptor
import plutoproject.framework.common.util.chat.UI_SELECT_SOUND
import plutoproject.framework.common.util.chat.component.splitLines
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.provider.timezone
import plutoproject.framework.paper.util.coroutine.withSync
import kotlin.time.Duration.Companion.seconds

val StatusButtonDescriptor = ButtonDescriptor {
    id = "status:status"
}

@Composable
@Suppress("FunctionName")
fun Status() {
    val player = LocalPlayer.current
    val timeZone = remember { player.timezone.toZoneId() }
    var statusMessage by remember { mutableStateOf<Component?>(null) }
    var promptMessage by remember { mutableStateOf(getPromptMessage()) }
    var showVersionMessage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            statusMessage = withSync { getStatusMessage() }
            promptMessage = getPromptMessage()
            delay(1.seconds)
        }
    }

    Item(
        material = Material.BRUSH,
        name = component {
            text("服务器信息") with mochaText
        },
        lore = buildList {
            if (!showVersionMessage) {
                if (statusMessage == null) {
                    add(component {
                        text("正在加载...") with mochaSubtext0
                    })
                    return@buildList
                }
                addAll(statusMessage!!.splitLines())
                add(Component.empty())
                addAll(promptMessage.splitLines())
                add(Component.empty())
                add(BUTTON_SERVER_STATUS_OPERATION_SHOW_VERSION)
            } else {
                addAll(getVersionMessage(timeZone).splitLines())
                add(Component.empty())
                add(BUTTON_SERVER_STATUS_OPERATION_HIDE_VERSION)
            }
        },
        modifier = Modifier.clickable {
            when (clickType) {
                ClickType.LEFT -> {
                    showVersionMessage = !showVersionMessage
                    player.playSound(UI_SELECT_SOUND)
                }

                else -> {}
            }
        }
    )
}
