package plutoproject.feature.whip.paper

import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Server
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.ShapelessRecipe

internal val WHIP_BASE_RECIPE_KEY = NamespacedKey(WHIP_NAMESPACE, "base")
internal val WHIP_UPGRADE_RECIPE_KEY = NamespacedKey(WHIP_NAMESPACE, "upgrade")
internal val WHIP_RECIPE_KEYS = listOf(WHIP_BASE_RECIPE_KEY, WHIP_UPGRADE_RECIPE_KEY)

internal enum class WhipRecipeKind {
    BASE,
    UPGRADE,
}

internal fun registerWhipRecipes(server: Server, config: WhipConfig) {
    server.addRecipe(buildBaseRecipe(config))
    server.addRecipe(buildUpgradeRecipe(config))
}

internal fun unregisterWhipRecipes(server: Server) {
    server.removeRecipe(WHIP_BASE_RECIPE_KEY)
    server.removeRecipe(WHIP_UPGRADE_RECIPE_KEY)
}

private fun buildBaseRecipe(config: WhipConfig): ShapelessRecipe =
    ShapelessRecipe(WHIP_BASE_RECIPE_KEY, createWhipItem(WhipLevel.I, config)).apply {
        addIngredient(Material.LEAD)
        addIngredient(Material.LEATHER)
        addIngredient(Material.LEATHER)
        addIngredient(Material.LEATHER)
    }

private fun buildUpgradeRecipe(config: WhipConfig): ShapelessRecipe =
    ShapelessRecipe(WHIP_UPGRADE_RECIPE_KEY, createWhipItem(WhipLevel.II, config)).apply {
        addIngredient(Material.LEAD)
        addIngredient(Material.LEATHER)
    }

internal fun whipRecipeKind(recipe: Recipe?): WhipRecipeKind? {
    val key = (recipe as? Keyed)?.key ?: return null
    return when (key) {
        WHIP_BASE_RECIPE_KEY -> WhipRecipeKind.BASE
        WHIP_UPGRADE_RECIPE_KEY -> WhipRecipeKind.UPGRADE
        else -> null
    }
}

internal fun isWhipRecipe(recipe: Recipe?): Boolean = whipRecipeKind(recipe) != null

internal sealed interface WhipCraftIngredients {
    val outputLevel: WhipLevel
    val slots: List<Int>
    val stacks: List<ItemStack>

    val maxCraftCount: Int
        get() = stacks.minOf(ItemStack::getAmount)

    data class Base(
        val leadSlot: Int,
        val lead: ItemStack,
        val leatherSlots: List<Int>,
        val leather: List<ItemStack>,
    ) : WhipCraftIngredients {
        override val outputLevel = WhipLevel.I
        override val slots = listOf(leadSlot) + leatherSlots
        override val stacks = listOf(lead) + leather
    }

    data class Upgrade(
        val whipSlot: Int,
        val whip: ItemStack,
        val leatherSlot: Int,
        val leather: ItemStack,
        override val outputLevel: WhipLevel,
    ) : WhipCraftIngredients {
        override val slots = listOf(whipSlot, leatherSlot)
        override val stacks = listOf(whip, leather)
    }
}

internal fun findWhipCraftIngredients(
    recipe: Recipe?,
    matrix: Array<ItemStack?>,
): WhipCraftIngredients? {
    return when (whipRecipeKind(recipe)) {
        WhipRecipeKind.BASE -> findBaseIngredients(matrix)
        WhipRecipeKind.UPGRADE -> findUpgradeIngredients(matrix)
        null -> null
    }
}

private fun findBaseIngredients(matrix: Array<ItemStack?>): WhipCraftIngredients.Base? {
    val present = matrix.mapIndexedNotNull { index, item ->
        item.takeUnless(ItemStack?::isEmptyForWhip)?.let { index to it }
    }
    if (present.size != 4) {
        return null
    }

    val leads = present.filter { (_, item) -> item.isOrdinaryLead() }
    val leather = present.filter { (_, item) -> item.type == Material.LEATHER }
    if (leads.size != 1 || leather.size != 3 || leads.size + leather.size != present.size) {
        return null
    }

    return WhipCraftIngredients.Base(
        leadSlot = leads.single().first,
        lead = leads.single().second,
        leatherSlots = leather.map { it.first },
        leather = leather.map { it.second },
    )
}

private fun findUpgradeIngredients(matrix: Array<ItemStack?>): WhipCraftIngredients.Upgrade? {
    val present = matrix.mapIndexedNotNull { index, item ->
        item.takeUnless(ItemStack?::isEmptyForWhip)?.let { index to it }
    }
    if (present.size != 2) {
        return null
    }

    val whip = present.filter { (_, item) -> item.whipLevelOrNull() != null }
    val leather = present.filter { (_, item) -> item.type == Material.LEATHER }
    if (whip.size != 1 || leather.size != 1 || whip.size + leather.size != present.size) {
        return null
    }

    val level = whip.single().second.whipLevelOrNull() ?: return null
    val nextLevel = WhipLevel.fromNumber(level.number + 1) ?: return null
    return WhipCraftIngredients.Upgrade(
        whipSlot = whip.single().first,
        whip = whip.single().second,
        leatherSlot = leather.single().first,
        leather = leather.single().second,
        outputLevel = nextLevel,
    )
}

internal fun consumeWhipIngredients(
    matrix: Array<ItemStack?>,
    ingredients: WhipCraftIngredients,
    amount: Int,
) {
    require(amount >= 0) { "amount must be non-negative" }
    ingredients.slots.forEach { slot ->
        val item = matrix[slot] ?: return@forEach
        val remaining = item.amount - amount
        matrix[slot] = if (remaining > 0) {
            item.clone().apply { this.amount = remaining }
        } else {
            null
        }
    }
}

private fun ItemStack?.isEmptyForWhip(): Boolean =
    this == null || type.isAir || amount <= 0
