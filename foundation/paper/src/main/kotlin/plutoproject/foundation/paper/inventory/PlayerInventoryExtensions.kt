package plutoproject.foundation.paper.inventory

import org.bukkit.inventory.PlayerInventory

val PlayerInventory.isFull: Boolean
    get() = !storageContents.contains(null)
