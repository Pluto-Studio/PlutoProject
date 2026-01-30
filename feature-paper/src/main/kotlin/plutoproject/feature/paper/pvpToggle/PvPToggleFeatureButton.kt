package plutoproject.feature.paper.pvpToggle

import androidx.compose.runtime.*
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.paper.api.menu.dsl.ButtonDescriptor
import plutoproject.feature.paper.api.pvpToggle.PvPToggle
import plutoproject.framework.common.util.chat.UI_TOGGLE_OFF_SOUND
import plutoproject.framework.common.util.chat.UI_TOGGLE_ON_SOUND
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.animations.spinnerAnimation
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.modifiers.Modifier

val PvPToggleFeatureButtonDescriptor = ButtonDescriptor {
    id = "pvp_toggle:pvp_toggle"
}

private enum class PvPToggleFeatureState {
    LOADING, ENABLED, DISABLED
}

@Composable
@Suppress("FunctionName")
fun PvPToggleFeatureButton() {
    val player = LocalPlayer.current
    var state by remember { mutableStateOf(PvPToggleFeatureState.LOADING) }

    LaunchedEffect(Unit) {
        state = if (PvPToggle.isPvPEnabled(player)) {
            PvPToggleFeatureState.ENABLED
        } else {
            PvPToggleFeatureState.DISABLED
        }
    }

    Item(
        material = Material.STONE_SPEAR,
        name = when (state) {
            PvPToggleFeatureState.LOADING -> Component
                .text("${spinnerAnimation()} ")
                .color(mochaSubtext0)
                .append(MENU_PVP_TOGGLE_FEATURE_LOADING)

            PvPToggleFeatureState.ENABLED -> MENU_PVP_TOGGLE_FEATURE_ENABLED
            PvPToggleFeatureState.DISABLED -> MENU_PVP_TOGGLE_FEATURE_DISABLED
        },
        enchantmentGlint = state == PvPToggleFeatureState.ENABLED,
        lore = when (state) {
            PvPToggleFeatureState.LOADING -> emptyList()
            PvPToggleFeatureState.ENABLED -> MENU_PVP_TOGGLE_FEATURE_LORE_ENABLED
            PvPToggleFeatureState.DISABLED -> MENU_PVP_TOGGLE_FEATURE_LORE_DISABLED
        },
        modifier = Modifier.clickable {
            if (clickType != ClickType.LEFT) {
                return@clickable
            }
            when (state) {
                PvPToggleFeatureState.LOADING -> return@clickable
                PvPToggleFeatureState.ENABLED -> {
                    PvPToggle.setPvPEnabled(player, false)
                    player.playSound(UI_TOGGLE_OFF_SOUND)
                    state = PvPToggleFeatureState.DISABLED
                }

                PvPToggleFeatureState.DISABLED -> {
                    PvPToggle.setPvPEnabled(player, true)
                    player.playSound(UI_TOGGLE_ON_SOUND)
                    state = PvPToggleFeatureState.ENABLED
                }
            }
        }
    )
}
