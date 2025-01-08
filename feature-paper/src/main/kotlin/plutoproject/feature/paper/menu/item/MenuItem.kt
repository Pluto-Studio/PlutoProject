package plutoproject.feature.paper.menu.item

import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.recipe.CraftingBookCategory
import org.bukkit.persistence.PersistentDataType
import plutoproject.framework.common.util.chat.palettes.mochaLavender
import plutoproject.framework.common.util.chat.palettes.mochaSubtext0
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.paper.util.plugin

private val key = NamespacedKey(plugin, "menu")

val ItemStack.isMenuItem: Boolean
    get() {
        return persistentDataContainer.has(key)
    }

object MenuItem : ItemStack(Material.BOOK) {
    init {
        editMeta {
            it.setEnchantmentGlintOverride(true)
            it.persistentDataContainer.set(key, PersistentDataType.BOOLEAN, true)
            it.displayName(component {
                text("手账") with mochaText without italic()
            })
            it.lore(buildList {
                add(component {
                    text("记录着未尽之事的书。") with mochaSubtext0 without italic()
                })
                add(component {
                    text("若不慎丢失的话，可以在工作台里再打造一本。") with mochaSubtext0 without italic()
                })
                add(Component.empty())
                add(component {
                    text("手持右键 ") with mochaLavender without italic()
                    text("打开手账") with mochaText without italic()
                })
            })
        }
    }
}

object MenuItemRecipe : ShapedRecipe(key, MenuItem) {
    init {
        shape("FFF", "FBF", "FFF")
        setIngredient('F', Material.FEATHER)
        setIngredient('B', Material.BOOK)
        category = CraftingBookCategory.EQUIPMENT
    }
}
