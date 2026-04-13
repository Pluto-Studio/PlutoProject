package plutoproject.feature.gallery.adapter.paper.listener

import org.bukkit.Material
import org.bukkit.block.Crafter
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.CrafterCraftEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.inventory.ItemStack
import plutoproject.feature.gallery.adapter.paper.isImageItem
import plutoproject.feature.gallery.adapter.paper.isImageItemCopyRecipe

@Suppress("UNUSED")
object CraftListener : Listener {
    @EventHandler
    fun onCraftItem(event: CraftItemEvent) {
        findImageItemCopyIngredients(event.inventory.matrix)
            ?.takeIf { isImageItemCopyRecipe(event.recipe) }
            ?.let { ingredients ->
                handleImageItemCopyCraft(event, ingredients)
                return
            }

        val ingredients = event.inventory.matrix.map { it ?: ItemStack.empty() }
        if (ingredients.any { it.isImageItem() }) {
            event.inventory.result = ItemStack.empty()
        }
    }

    @EventHandler
    fun onPrepareItemCraft(event: PrepareItemCraftEvent) {
        if (isImageItemCopyRecipe(event.recipe)) {
            event.inventory.result = findImageItemCopyIngredients(event.inventory.matrix)
                ?.imageItem
                ?.cloneSingle()
                ?: ItemStack.empty()
            return
        }

        val ingredients = event.inventory.matrix.map { it ?: ItemStack.empty() }
        if (ingredients.any { it.isImageItem() }) {
            event.inventory.result = ItemStack.empty()
        }
    }

    @EventHandler
    fun onCrafterCraft(event: CrafterCraftEvent) {
        val crafter = event.block.state as? Crafter ?: return
        val ingredients = crafter.inventory.map { it ?: ItemStack.empty() }
        if (ingredients.any { it.isImageItem() }) {
            event.isCancelled = true
        }
    }

    private fun handleImageItemCopyCraft(
        event: CraftItemEvent,
        ingredients: ImageItemCopyIngredients,
    ) {
        event.isCancelled = true
        event.result = Event.Result.DENY

        val player = event.whoClicked as? Player ?: return
        val craftedItem = ingredients.imageItem.cloneSingle()

        if (event.isShiftClick) {
            craftImageItemsToInventory(player, event.inventory, ingredients, craftedItem)
        } else if (event.click.isLeftClick || event.click.isRightClick) {
            craftImageItemToCursor(player, event.inventory, ingredients, craftedItem)
        }

        player.updateInventory()
    }

    private fun craftImageItemToCursor(
        player: Player,
        inventory: org.bukkit.inventory.CraftingInventory,
        ingredients: ImageItemCopyIngredients,
        craftedItem: ItemStack,
    ) {
        val cursor = player.itemOnCursor
        if (!canPlaceOnCursor(cursor, craftedItem)) {
            updateCraftingResult(inventory)
            return
        }

        val newCursor = if (cursor.isEmpty()) {
            craftedItem
        } else {
            cursor.clone().apply { amount += craftedItem.amount }
        }

        player.setItemOnCursor(newCursor)
        val matrix = inventory.matrix
        consumeIngredient(matrix, ingredients.inkSlot, 1)
        consumeIngredient(matrix, ingredients.featherSlot, 1)
        inventory.matrix = matrix
        updateCraftingResult(inventory)
    }

    private fun craftImageItemsToInventory(
        player: Player,
        inventory: org.bukkit.inventory.CraftingInventory,
        ingredients: ImageItemCopyIngredients,
        craftedItem: ItemStack,
    ) {
        val craftCount = minOf(ingredients.ink.amount, ingredients.feather.amount)
        if (craftCount <= 0) {
            updateCraftingResult(inventory)
            return
        }

        val maxStackSize = craftedItem.maxStackSize
        var remaining = craftCount
        val itemsToAdd = mutableListOf<ItemStack>()
        while (remaining > 0) {
            val stackAmount = minOf(remaining, maxStackSize)
            itemsToAdd += craftedItem.clone().apply { amount = stackAmount }
            remaining -= stackAmount
        }

        val leftovers = player.inventory.addItem(*itemsToAdd.toTypedArray())
        val craftedAmount = craftCount - leftovers.values.sumOf(ItemStack::getAmount)
        if (craftedAmount <= 0) {
            updateCraftingResult(inventory)
            return
        }

        val matrix = inventory.matrix
        consumeIngredient(matrix, ingredients.inkSlot, craftedAmount)
        consumeIngredient(matrix, ingredients.featherSlot, craftedAmount)
        inventory.matrix = matrix
        updateCraftingResult(inventory)
    }

    private fun updateCraftingResult(inventory: org.bukkit.inventory.CraftingInventory) {
        inventory.result = findImageItemCopyIngredients(inventory.matrix)
            ?.imageItem
            ?.cloneSingle()
            ?: ItemStack.empty()
    }

    private fun canPlaceOnCursor(cursor: ItemStack, craftedItem: ItemStack): Boolean {
        if (cursor.isEmpty) {
            return true
        }

        if (!cursor.isSimilar(craftedItem)) {
            return false
        }

        return cursor.amount + craftedItem.amount <= cursor.maxStackSize
    }

    private fun consumeIngredient(matrix: Array<ItemStack?>, slot: Int, amount: Int) {
        val item = matrix[slot] ?: return
        val remainingAmount = item.amount - amount
        matrix[slot] = if (remainingAmount > 0) {
            item.clone().apply { this.amount = remainingAmount }
        } else {
            null
        }
    }

    private fun findImageItemCopyIngredients(matrix: Array<ItemStack?>): ImageItemCopyIngredients? {
        var imageItemSlot = -1
        var imageItem: ItemStack? = null
        var inkSlot = -1
        var ink: ItemStack? = null
        var featherSlot = -1
        var feather: ItemStack? = null

        matrix.forEachIndexed { index, item ->
            if (item.isEmpty()) {
                return@forEachIndexed
            }

            val ingredient = item!!

            when {
                ingredient.isImageItem() && imageItem == null -> {
                    imageItemSlot = index
                    imageItem = ingredient
                }

                ingredient.type == Material.FEATHER && feather == null -> {
                    featherSlot = index
                    feather = ingredient
                }

                (ingredient.type == Material.INK_SAC || ingredient.type == Material.GLOW_INK_SAC) && ink == null -> {
                    inkSlot = index
                    ink = ingredient
                }

                else -> return null
            }
        }

        return if (imageItem != null && ink != null && feather != null) {
            ImageItemCopyIngredients(
                imageItemSlot = imageItemSlot,
                imageItem = imageItem,
                inkSlot = inkSlot,
                ink = ink,
                featherSlot = featherSlot,
                feather = feather,
            )
        } else {
            null
        }
    }

    private fun ItemStack?.isEmpty(): Boolean {
        return this == null || type.isAir || amount <= 0
    }

    private fun ItemStack.cloneSingle(): ItemStack {
        return clone().apply { amount = 1 }
    }

    private data class ImageItemCopyIngredients(
        val imageItemSlot: Int,
        val imageItem: ItemStack,
        val inkSlot: Int,
        val ink: ItemStack,
        val featherSlot: Int,
        val feather: ItemStack,
    )
}
