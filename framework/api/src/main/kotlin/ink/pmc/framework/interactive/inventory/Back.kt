package ink.pmc.framework.interactive.inventory

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import ink.pmc.framework.interactive.inventory.click.clickable
import ink.pmc.framework.chat.UI_BACK
import org.bukkit.Material

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