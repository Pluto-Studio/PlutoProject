package plutoproject.feature.paper.status

import androidx.compose.runtime.*
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import org.bukkit.Material
import plutoproject.feature.paper.api.menu.dsl.ButtonDescriptor
import plutoproject.framework.common.util.chat.component.splitLines
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.util.coroutine.withSync
import kotlin.time.Duration.Companion.seconds

val StatusButtonDescriptor = ButtonDescriptor {
    id = "status:status"
}

@Composable
@Suppress("FunctionName")
fun Status() {
    var statusMessage by remember { mutableStateOf<Component?>(null) }
    var promptMessage by remember { mutableStateOf(getPromptMessage()) }

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
            text("服务器状态信息") with mochaText without italic()
        },
        lore = buildList {
            if (statusMessage == null) {
                add(component {
                    text("正在加载...") with mochaSubtext0 without italic()
                })
                return@buildList
            }
            addAll(statusMessage!!.splitLines())
            add(Component.empty())
            addAll(promptMessage.splitLines())
        }
    )
}
