package plutoproject.framework.paper.api.interactive.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import plutoproject.framework.paper.api.interactive.layout.Layout
import plutoproject.framework.paper.api.interactive.canvas.Canvas
import plutoproject.framework.paper.api.interactive.measuring.MeasureResult
import plutoproject.framework.paper.api.interactive.measuring.Renderer
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.modifiers.sizeIn
import plutoproject.framework.paper.api.interactive.node.BaseInventoryNode

@Composable
fun Item(itemStack: ItemStack, modifier: Modifier = Modifier) {
    Layout(
        measurePolicy = { _, constraints ->
            MeasureResult(constraints.minWidth, constraints.minHeight) {}
        },
        renderer = object : Renderer {
            override fun Canvas.render(node: BaseInventoryNode) {
                for (x in 0 until node.width)
                    for (y in 0 until node.height)
                        set(x, y, itemStack)
            }
        },
        modifier = Modifier.sizeIn(minWidth = 1, minHeight = 1).then(modifier)
    )
}

@Composable
fun Item(
    material: Material,
    name: Component = Component.empty(),
    amount: Int = 1,
    lore: List<Component> = emptyList(),
    isHideTooltip: Boolean = false,
    enchantmentGlint: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val rememberName = remember(name) { name }
    val rememberLore = remember(lore) { lore }

    val item = remember(material, name, amount, lore) {
        ItemStack(material, amount).apply {
            editMeta {
                it.displayName(rememberName)
                it.lore(rememberLore)
                it.isHideTooltip = isHideTooltip
                it.setEnchantmentGlintOverride(enchantmentGlint)
            }
        }
    }

    Item(item, modifier)
}
