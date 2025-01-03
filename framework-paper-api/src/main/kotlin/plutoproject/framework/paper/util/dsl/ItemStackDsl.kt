package plutoproject.framework.paper.util.dsl

import ink.pmc.advkt.component.RootComponentKt
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

typealias MetaModifier = ItemMeta.() -> Unit

class ItemStackDsl(private var material: Material, private val amount: Int) {
    var displayName: Component? = null
    private val lore = mutableListOf<Component>()
    private val enchantments = mutableMapOf<Enchantment, Int>()
    private val itemFlags = mutableSetOf<ItemFlag>()
    private var metaModifier: MetaModifier = {}

    fun displayName(component: RootComponentKt.() -> Unit) {
        displayName = RootComponentKt().apply(component).build()
    }

    fun lore(component: Component) {
        lore.add(component)
    }

    fun lore(component: RootComponentKt.() -> Unit) {
        lore.add(RootComponentKt().apply(component).build())
    }

    fun lore(components: Collection<Component>) {
        lore.addAll(components)
    }

    fun enchantment(enchantment: Enchantment, level: Int) {
        enchantments[enchantment] = level
    }

    fun enchantment(map: Map<Enchantment, Int>) {
        enchantments.putAll(map)
    }

    fun itemFlag(itemFlag: ItemFlag) {
        itemFlags.add(itemFlag)
    }

    fun itemFlag(vararg itemFlags: ItemFlag) {
        this.itemFlags.addAll(itemFlags)
    }

    fun itemFlag(itemFlags: Collection<ItemFlag>) {
        this.itemFlags.addAll(itemFlags)
    }

    fun meta(modifier: MetaModifier) {
        this.metaModifier = modifier
    }

    fun build(): ItemStack =
        ItemStack(material, amount)
            .apply {
                editMeta {
                    it.displayName(displayName)
                    it.lore(this@ItemStackDsl.lore)
                    it.metaModifier()
                }
                addEnchantments(this@ItemStackDsl.enchantments)
                addItemFlags(*this@ItemStackDsl.itemFlags.toTypedArray())
            }
}

fun ItemStack(material: Material, amount: Int = 1, block: ItemStackDsl.() -> Unit): ItemStack {
    return ItemStackDsl(material, amount).apply(block).build()
}

fun ItemStack(material: Material, amount: Int = 1): ItemStack {
    return ItemStackDsl(material, amount).build()
}

fun Inventory.addItem(material: Material, amount: Int = 1, block: ItemStackDsl.() -> Unit) {
    addItem(ItemStack(material, amount, block))
}

fun Inventory.addItem(material: Material, amount: Int = 1) {
    addItem(plutoproject.framework.paper.util.dsl.ItemStack(material, amount))
}
