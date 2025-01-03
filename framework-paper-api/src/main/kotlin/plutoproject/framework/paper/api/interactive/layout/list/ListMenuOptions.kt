package plutoproject.framework.paper.api.interactive.layout.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.kyori.adventure.text.Component
import org.bukkit.Material

class ListMenuOptions {
    var title by mutableStateOf(Component.empty())
    var rows by mutableStateOf(6)
    var topBorder by mutableStateOf(true)
    var bottomBorder by mutableStateOf(true)
    var leftBorder by mutableStateOf(true)
    var rightBorder by mutableStateOf(true)
    var background by mutableStateOf(true)
    var centerBackground by mutableStateOf(false)
    var previousTurnerIcon by mutableStateOf(Material.ARROW)
    var nextTurnerIcon by mutableStateOf(Material.ARROW)
}
