package plutoproject.feature.paper.sit

import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player

internal fun Player.playSitSound() {
    val leggings = inventory.leggings
    val sound = if (leggings == null) {
        Sound.ITEM_ARMOR_EQUIP_GENERIC
    } else when (leggings.type) {
        Material.LEATHER_LEGGINGS -> Sound.ITEM_ARMOR_EQUIP_LEATHER
        Material.CHAINMAIL_LEGGINGS -> Sound.ITEM_ARMOR_EQUIP_CHAIN
        Material.IRON_LEGGINGS -> Sound.ITEM_ARMOR_EQUIP_IRON
        Material.GOLDEN_LEGGINGS -> Sound.ITEM_ARMOR_EQUIP_GOLD
        Material.DIAMOND_LEGGINGS -> Sound.ITEM_ARMOR_EQUIP_DIAMOND
        Material.NETHERITE_LEGGINGS -> Sound.ITEM_ARMOR_EQUIP_NETHERITE
        else -> Sound.ITEM_ARMOR_EQUIP_GENERIC
    }
    world.playSound(location, sound, SoundCategory.BLOCKS, 1f, 1f)
}
