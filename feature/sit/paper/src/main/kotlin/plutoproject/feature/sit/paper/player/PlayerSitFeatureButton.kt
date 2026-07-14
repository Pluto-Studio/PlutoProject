package plutoproject.feature.sit.paper.player

import androidx.compose.runtime.*
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.menu.api.paper.dsl.ButtonDescriptor
import plutoproject.feature.sit.api.paper.player.PlayerSit
import plutoproject.feature.sit.paper.*
import plutoproject.feature.sit.paper.player.PlayerSitFeatureState.*
import plutoproject.foundation.common.text.UI_SUCCEED_SOUND
import plutoproject.foundation.common.text.UI_TOGGLE_OFF_SOUND
import plutoproject.foundation.common.text.UI_TOGGLE_ON_SOUND
import plutoproject.foundation.common.text.mochaSubtext0
import plutoproject.capability.interactive.api.LocalPlayer
import plutoproject.capability.interactive.api.animations.spinnerAnimation
import plutoproject.capability.interactive.api.click.clickable
import plutoproject.capability.interactive.api.components.Item
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.kernel.api.koinInject

private val playerSit by koinInject<PlayerSit>()

val PlayerSitFeatureButtonDescriptor = ButtonDescriptor {
    id = "sit:player_sit"
}

private enum class PlayerSitFeatureState {
    LOADING, ENABLED, DISABLED
}

@Composable
@Suppress("FunctionName")
fun PlayerSitToggle() {
    val player = LocalPlayer.current
    var state by remember { mutableStateOf(LOADING) }

    LaunchedEffect(Unit) {
        state = if (playerSit.isFeatureEnabled(player)) {
            ENABLED
        } else {
            DISABLED
        }
    }

    Item(
        material = Material.ARMOR_STAND,
        name = when (state) {
            LOADING -> Component
                .text("${spinnerAnimation()} ")
                .color(mochaSubtext0)
                .append(MENU_PLAYER_SIT_FEATURE_LOADING)

            ENABLED -> MENU_PLAYER_SIT_FEATURE_ENABLED
            DISABLED -> MENU_PLAYER_SIT_FEATURE_DISABLED
        },
        enchantmentGlint = state == ENABLED,
        lore = when (state) {
            LOADING -> emptyList()
            ENABLED -> MENU_PLAYER_SIT_FEATURE_LORE_ENABLED
            DISABLED -> MENU_PLAYER_SIT_FEATURE_LORE_DISABLED
        },
        modifier = Modifier.clickable {
            if (clickType != ClickType.LEFT) {
                return@clickable
            }
            when (state) {
                LOADING -> return@clickable
                ENABLED -> {
                    playerSit.toggleFeature(player, false)
                    player.playSound(UI_TOGGLE_OFF_SOUND)
                    state = DISABLED
                }

                DISABLED -> {
                    playerSit.toggleFeature(player, true)
                    player.playSound(UI_TOGGLE_ON_SOUND)
                    state = ENABLED
                }
            }
        }
    )
}
