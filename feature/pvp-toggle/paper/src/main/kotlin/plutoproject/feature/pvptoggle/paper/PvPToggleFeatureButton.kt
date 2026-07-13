package plutoproject.feature.pvptoggle.paper

import androidx.compose.runtime.*
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import io.papermc.paper.datacomponent.item.TooltipDisplay
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import plutoproject.feature.menu.api.paper.dsl.ButtonDescriptor
import plutoproject.feature.pvptoggle.api.paper.PvPToggle
import plutoproject.foundation.common.text.UI_TOGGLE_OFF_SOUND
import plutoproject.foundation.common.text.UI_TOGGLE_ON_SOUND
import plutoproject.foundation.common.text.mochaSubtext0
import plutoproject.capability.interactive.api.LocalPlayer
import plutoproject.capability.interactive.api.animations.spinnerAnimation
import plutoproject.capability.interactive.api.click.clickable
import plutoproject.capability.interactive.api.components.Item
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.kernel.api.koinInject

private val pvpToggle by koinInject<PvPToggle>()

val PvPToggleFeatureButtonDescriptor = ButtonDescriptor {
    id = "pvp_toggle:pvp_toggle"
}

private enum class PvPToggleFeatureState {
    LOADING, ENABLED, DISABLED
}

@Composable
@Suppress("FunctionName", "UnstableApiUsage")
fun PvPToggleFeatureButton() {
    val player = LocalPlayer.current
    var state by remember { mutableStateOf(PvPToggleFeatureState.LOADING) }

    LaunchedEffect(Unit) {
        state = if (pvpToggle.isPvPEnabled(player)) {
            PvPToggleFeatureState.ENABLED
        } else {
            PvPToggleFeatureState.DISABLED
        }
    }

    Item(
        itemStack = ItemStack(Material.STONE_SPEAR).apply {
            val name = when (state) {
                PvPToggleFeatureState.LOADING -> Component
                    .text("${spinnerAnimation()} ")
                    .color(mochaSubtext0)
                    .append(MENU_PVP_TOGGLE_FEATURE_LOADING)

                PvPToggleFeatureState.ENABLED -> MENU_PVP_TOGGLE_FEATURE_ENABLED
                PvPToggleFeatureState.DISABLED -> MENU_PVP_TOGGLE_FEATURE_DISABLED
            }
            val lore = when (state) {
                PvPToggleFeatureState.LOADING -> emptyList()
                PvPToggleFeatureState.ENABLED -> MENU_PVP_TOGGLE_FEATURE_LORE_ENABLED
                PvPToggleFeatureState.DISABLED -> MENU_PVP_TOGGLE_FEATURE_LORE_DISABLED
            }
            setData(DataComponentTypes.ITEM_NAME, name)
            setData(DataComponentTypes.LORE, ItemLore.lore(lore))
            setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, state == PvPToggleFeatureState.ENABLED)
            setData(
                DataComponentTypes.TOOLTIP_DISPLAY,
                TooltipDisplay.tooltipDisplay().addHiddenComponents(DataComponentTypes.ATTRIBUTE_MODIFIERS)
            )
        },
        modifier = Modifier.clickable {
            if (clickType != ClickType.LEFT) {
                return@clickable
            }
            when (state) {
                PvPToggleFeatureState.LOADING -> return@clickable
                PvPToggleFeatureState.ENABLED -> {
                    pvpToggle.setPvPEnabled(player, false)
                    player.playSound(UI_TOGGLE_OFF_SOUND)
                    state = PvPToggleFeatureState.DISABLED
                }

                PvPToggleFeatureState.DISABLED -> {
                    pvpToggle.setPvPEnabled(player, true)
                    player.playSound(UI_TOGGLE_ON_SOUND)
                    state = PvPToggleFeatureState.ENABLED
                }
            }
        }
    )
}
