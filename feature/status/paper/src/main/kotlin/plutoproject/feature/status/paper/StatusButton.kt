package plutoproject.feature.status.paper

import androidx.compose.runtime.*
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.text
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import plutoproject.capability.geoip.api.GeoIpConnection
import plutoproject.capability.interactive.api.LocalPlayer
import plutoproject.capability.interactive.api.animations.spinnerAnimation
import plutoproject.capability.interactive.api.click.clickable
import plutoproject.capability.interactive.api.components.Item
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.foundation.common.text.UI_SELECT_SOUND
import plutoproject.foundation.common.text.mochaSubtext0
import plutoproject.foundation.common.text.mochaText
import plutoproject.foundation.common.text.splitLines
import plutoproject.foundation.common.time.LocalZoneId
import plutoproject.kernel.api.currentModuleContext
import plutoproject.kernel.api.getService
import plutoproject.kernel.api.paper.PaperModuleContext
import java.time.ZoneId
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.seconds

private val serverContext
    get() = (currentModuleContext() as PaperModuleContext).plugin.minecraftDispatcher

private fun Player.timezone(): ZoneId = address?.let { address ->
    currentModuleContext().services.getService<GeoIpConnection>()
        .database.tryCity(address.address).getOrNull()?.location?.timeZone?.let(ZoneId::of)
} ?: LocalZoneId

@Composable
@Suppress("FunctionName")
fun Status() {
    val player = LocalPlayer.current
    val timeZone = remember(player) { player.timezone() }
    var statusMessage by remember { mutableStateOf<Component?>(null) }
    var promptMessage by remember { mutableStateOf(getPromptMessage()) }
    var showVersionMessage by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            statusMessage = withContext(serverContext) { getStatusMessage() }
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
                    add(Component.text("${spinnerAnimation()} 正在加载...").color(mochaSubtext0))
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
