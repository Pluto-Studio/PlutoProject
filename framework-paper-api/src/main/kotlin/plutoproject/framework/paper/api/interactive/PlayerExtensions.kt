package plutoproject.framework.paper.api.interactive

import androidx.compose.runtime.Composable
import org.bukkit.entity.Player

fun Player.startInventory(content: @Composable () -> Unit) {
    GuiManager.startInventory(this) {
        content()
    }
}

fun Player.startScreen(screen: InteractiveScreen) {
    GuiManager.startScreen(this, screen)
}
