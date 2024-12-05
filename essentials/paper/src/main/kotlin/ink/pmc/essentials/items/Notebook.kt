package ink.pmc.essentials.items

import ink.pmc.advkt.component.italic
import ink.pmc.advkt.component.text
import ink.pmc.essentials.plugin
import ink.pmc.framework.utils.dsl.itemStack
import ink.pmc.framework.utils.visual.mochaLavender
import ink.pmc.framework.utils.visual.mochaSubtext0
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

private val key = NamespacedKey(plugin, "notebook_item")

val NOTEBOOK_ITEM = itemStack(Material.BOOK) {
    meta {
        setEnchantmentGlintOverride(true)
        persistentDataContainer.set(key, PersistentDataType.BOOLEAN, true)
    }
    displayName {
        text("手账") with mochaLavender without italic()
    }
    lore {
        text("记录着未尽之事的书。") with mochaSubtext0 without italic()
    }
    lore {
        text("若不慎丢失的话，可以在工作台里再打造一本。") with mochaSubtext0 without italic()
    }
}

val ItemStack.isNotebookItem: Boolean
    get() {
        return itemMeta?.persistentDataContainer?.getOrDefault(
            key,
            PersistentDataType.BOOLEAN,
            false
        ) ?: false
    }