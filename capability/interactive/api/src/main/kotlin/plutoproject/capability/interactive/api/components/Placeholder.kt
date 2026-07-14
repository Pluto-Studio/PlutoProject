package plutoproject.capability.interactive.api.components

import androidx.compose.runtime.Composable
import org.bukkit.Material
import plutoproject.capability.interactive.api.modifiers.Modifier

@Composable
@Suppress("FunctionName")
fun Placeholder(modifier: Modifier = Modifier) {
    Item(
        material = Material.GRAY_STAINED_GLASS_PANE,
        isHideTooltip = true,
        modifier = modifier
    )
}
