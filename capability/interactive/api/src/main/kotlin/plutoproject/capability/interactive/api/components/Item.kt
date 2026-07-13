package plutoproject.capability.interactive.api.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import plutoproject.foundation.common.text.explicitRemoveItalic
import plutoproject.capability.interactive.api.canvas.Canvas
import plutoproject.capability.interactive.api.layout.Layout
import plutoproject.capability.interactive.api.measuring.MeasureResult
import plutoproject.capability.interactive.api.measuring.Renderer
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.capability.interactive.api.modifiers.sizeIn
import plutoproject.capability.interactive.api.node.BaseInventoryNode

@Composable
fun Item(itemStack: ItemStack, modifier: Modifier = Modifier) {
    val explicitRemovedItalic = itemStack.clone()
        .apply {
            editMeta { meta ->
                meta.displayName(meta.displayName()?.explicitRemoveItalic())
                meta.lore(meta.lore()?.map { comp -> comp.explicitRemoveItalic() })
            }
        }
    Layout(
        measurePolicy = { _, constraints ->
            MeasureResult(constraints.minWidth, constraints.minHeight) {}
        },
        renderer = object : Renderer {
            override fun Canvas.render(node: BaseInventoryNode) {
                for (x in 0 until node.width)
                    for (y in 0 until node.height)
                        set(x, y, explicitRemovedItalic)
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
                it.displayName(rememberName.explicitRemoveItalic())
                it.lore(rememberLore.map { comp -> comp.explicitRemoveItalic() })
                it.isHideTooltip = isHideTooltip
                it.setEnchantmentGlintOverride(enchantmentGlint)
            }
        }
    }

    Item(item, modifier)
}
