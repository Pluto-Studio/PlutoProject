package plutoproject.framework.paper.api.interactive.components

import androidx.compose.runtime.Composable
import org.bukkit.Material
import plutoproject.framework.paper.api.interactive.modifiers.Modifier

@Composable
@Suppress("FunctionName")
fun Placeholder(modifier: Modifier = Modifier) {
    Item(
        material = Material.GRAY_STAINED_GLASS_PANE,
        isHideTooltip = true,
        modifier = modifier
    )
}
