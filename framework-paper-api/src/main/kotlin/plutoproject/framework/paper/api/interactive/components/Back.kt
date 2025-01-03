package plutoproject.framework.paper.api.interactive.components

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import org.bukkit.Material
import plutoproject.framework.common.util.chat.MessageConstants
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.click.clickable

@Composable
@Suppress("FunctionName")
fun Back() {
    val navigator = LocalNavigator.current
    if (navigator == null || !navigator.canPop) return
    Item(
        material = Material.YELLOW_STAINED_GLASS_PANE,
        name = MessageConstants.UI.back,
        modifier = Modifier.clickable {
            navigator.pop()
        }
    )
}
