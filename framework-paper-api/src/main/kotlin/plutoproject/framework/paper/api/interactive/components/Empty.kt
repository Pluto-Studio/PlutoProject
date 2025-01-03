package plutoproject.framework.paper.api.interactive.components

import androidx.compose.runtime.Composable
import org.bukkit.Material
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.modifiers.height
import plutoproject.framework.paper.api.interactive.modifiers.width

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
