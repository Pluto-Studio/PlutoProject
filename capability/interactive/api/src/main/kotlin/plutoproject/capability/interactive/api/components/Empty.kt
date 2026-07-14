package plutoproject.capability.interactive.api.components

import androidx.compose.runtime.Composable
import org.bukkit.Material
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.capability.interactive.api.modifiers.height
import plutoproject.capability.interactive.api.modifiers.width

@Composable
@Suppress("FunctionName")
fun Empty(modifier: Modifier = Modifier) {
    Item(material = Material.AIR, modifier = modifier)
}

@Composable
@Suppress("FunctionName")
fun ItemEmpty() {
    Empty(modifier = Modifier.width(1).height(1))
}
