package ink.pmc.essentials.recipes

import ink.pmc.essentials.plugin
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Server
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapedRecipe

private fun key(name: String): NamespacedKey {
    return NamespacedKey(plugin, "vanilla_extend_$name")
}

private val elytra = ShapedRecipe(key("elytra"), ItemStack(Material.ELYTRA, 1))
    .apply {
        shape("F F", "PLP", "F F")
        setIngredient('F', Material.FEATHER)
        setIngredient('P', Material.PHANTOM_MEMBRANE)
        setIngredient('L', Material.LEATHER)
    }

internal val vanillaExtendRecipes = listOf(elytra)

internal fun Server.registerVanillaExtend() {
    vanillaExtendRecipes.forEach { addRecipe(it) }
}