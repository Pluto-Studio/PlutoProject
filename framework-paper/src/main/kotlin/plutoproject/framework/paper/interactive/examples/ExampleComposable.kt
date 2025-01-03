package plutoproject.framework.paper.interactive.examples

import androidx.compose.runtime.Composable
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import net.kyori.adventure.text.Component
import org.bukkit.Material
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.paper.api.interactive.canvas.Chest
import plutoproject.framework.paper.api.interactive.click.clickable
import plutoproject.framework.paper.api.interactive.components.Item
import plutoproject.framework.paper.api.interactive.layout.VerticalGrid
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.modifiers.fillMaxSize

@Composable
@Suppress("FunctionName")
fun ExampleComposable() {
    Chest(title = Component.text("ExampleScreen3"), modifier = Modifier.fillMaxSize()) {
        VerticalGrid(modifier = Modifier.fillMaxSize()) {
            repeat(54) {
                Item(
                    material = Material.PAPER,
                    name = component {
                        text("测试物品") with mochaText without italic()
                    },
                    modifier = Modifier.clickable {
                        whoClicked.send {
                            text("你点击了测试物品") with mochaText
                        }
                    }
                )
            }
        }
    }
}
