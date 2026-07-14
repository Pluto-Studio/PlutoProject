package plutoproject.capability.interactive.api

import androidx.compose.runtime.Composable
import org.bukkit.entity.Player

fun Player.startInventory(manager: GuiManager, content: @Composable () -> Unit) {
    manager.startInventory(this) {
        content()
    }
}

fun Player.startScreen(manager: GuiManager, screen: InteractiveScreen) {
    manager.startScreen(this, screen)
}
