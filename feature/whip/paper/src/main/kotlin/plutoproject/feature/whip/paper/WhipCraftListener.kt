package plutoproject.feature.whip.paper

import org.bukkit.block.Crafter
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.CrafterCraftEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.ItemStack

class WhipCraftListener(
    private val config: WhipConfig,
) : Listener {
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        event.player.discoverRecipes(WHIP_RECIPE_KEYS)
    }

    @EventHandler
    fun onPrepareItemCraft(event: PrepareItemCraftEvent) {
        if (!isWhipRecipe(event.recipe)) {
            return
        }

        val ingredients = findWhipCraftIngredients(event.recipe, event.inventory.matrix)
        event.inventory.result = ingredients?.let { createWhipItem(it.outputLevel, config) } ?: ItemStack.empty()
    }

    @EventHandler
    fun onCraftItem(event: CraftItemEvent) {
        if (!isWhipRecipe(event.recipe)) {
            return
        }

        event.isCancelled = true
        event.result = Event.Result.DENY

        val ingredients = findWhipCraftIngredients(event.recipe, event.inventory.matrix)
        if (ingredients == null) {
            event.inventory.result = ItemStack.empty()
            return
        }

        val player = event.whoClicked as? Player ?: return
        val craftedItem = createWhipItem(ingredients.outputLevel, config)
        when {
            event.isShiftClick -> craftToInventory(player, event.inventory, event.recipe, ingredients, craftedItem)
            event.click.isLeftClick || event.click.isRightClick ->
                craftToCursor(player, event.inventory, event.recipe, ingredients, craftedItem)
        }
        player.updateInventory()
    }

    @EventHandler
    fun onCrafterCraft(event: CrafterCraftEvent) {
        if (!isWhipRecipe(event.recipe)) {
            return
        }

        val crafter = event.block.state as? Crafter ?: run {
            event.isCancelled = true
            return
        }
        val ingredients = findWhipCraftIngredients(event.recipe, crafter.inventory.contents)
        if (ingredients == null) {
            event.isCancelled = true
            return
        }

        event.result = createWhipItem(ingredients.outputLevel, config)
    }

    private fun craftToCursor(
        player: Player,
        inventory: CraftingInventory,
        recipe: org.bukkit.inventory.Recipe,
        ingredients: WhipCraftIngredients,
        craftedItem: ItemStack,
    ) {
        if (!player.itemOnCursor.isEmpty) {
            updateResult(inventory, recipe)
            return
        }

        player.setItemOnCursor(craftedItem)
        val matrix = inventory.matrix
        consumeWhipIngredients(matrix, ingredients, 1)
        inventory.matrix = matrix
        updateResult(inventory, recipe)
    }

    private fun craftToInventory(
        player: Player,
        inventory: CraftingInventory,
        recipe: org.bukkit.inventory.Recipe,
        ingredients: WhipCraftIngredients,
        craftedItem: ItemStack,
    ) {
        val craftCount = ingredients.maxCraftCount
        if (craftCount <= 0) {
            updateResult(inventory, recipe)
            return
        }

        var remaining = craftCount
        val items = buildList {
            while (remaining > 0) {
                val amount = minOf(remaining, craftedItem.maxStackSize)
                add(craftedItem.clone().apply { this.amount = amount })
                remaining -= amount
            }
        }
        val leftovers = player.inventory.addItem(*items.toTypedArray())
        val craftedAmount = (craftCount - leftovers.values.sumOf(ItemStack::getAmount)).coerceAtLeast(0)
        if (craftedAmount > 0) {
            val matrix = inventory.matrix
            consumeWhipIngredients(matrix, ingredients, craftedAmount)
            inventory.matrix = matrix
        }
        updateResult(inventory, recipe)
    }

    private fun updateResult(inventory: CraftingInventory, recipe: org.bukkit.inventory.Recipe) {
        val ingredients = findWhipCraftIngredients(recipe, inventory.matrix)
        inventory.result = ingredients?.let { createWhipItem(it.outputLevel, config) } ?: ItemStack.empty()
    }
}
