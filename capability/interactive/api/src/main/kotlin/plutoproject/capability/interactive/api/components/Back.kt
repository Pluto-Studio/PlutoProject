package plutoproject.capability.interactive.api.components

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import org.bukkit.Material
import plutoproject.capability.interactive.api.click.clickable
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.foundation.common.text.UI_BACK

@Composable
@Suppress("FunctionName")
fun Back() {
    val navigator = LocalNavigator.current
    if (navigator == null || !navigator.canPop) return
    Item(
        material = Material.YELLOW_STAINED_GLASS_PANE,
        name = UI_BACK,
        modifier = Modifier.clickable {
            navigator.pop()
        }
    )
}
