package plutoproject.feature.paper.sit.player

import androidx.compose.runtime.*
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import plutoproject.feature.paper.api.menu.dsl.ButtonDescriptor
import plutoproject.feature.paper.api.sit.player.PlayerSit
import plutoproject.feature.paper.sit.*
import plutoproject.feature.paper.sit.player.PlayerSitFeatureState.*
import plutoproject.framework.common.util.chat.UI_SUCCEED_SOUND
import plutoproject.framework.paper.api.interactive.LocalPlayer
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.modifiers.Modifier

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
        state = if (PlayerSit.isFeatureEnabled(player)) {
            ENABLED
        } else {
            DISABLED
        }
    }

    Item(
        material = Material.ARMOR_STAND,
        name = when (state) {
            LOADING -> MENU_PLAYER_SIT_FEATURE_LOADING
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
                    PlayerSit.toggleFeature(player, false)
                    state = DISABLED
                }

                DISABLED -> {
                    PlayerSit.toggleFeature(player, true)
                    state = ENABLED
                }
            }
            player.playSound(UI_SUCCEED_SOUND)
        }
    )
}
