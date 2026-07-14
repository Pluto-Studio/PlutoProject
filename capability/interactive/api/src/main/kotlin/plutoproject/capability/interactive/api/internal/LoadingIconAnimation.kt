package plutoproject.capability.interactive.api.internal

import org.bukkit.Material
import plutoproject.foundation.common.animation.SimpleObjectAnimation

class LoadingIconAnimation : SimpleObjectAnimation<Material>() {
    override val frames: Array<Material> = arrayOf(
        Material.HOPPER_MINECART,
        Material.CHEST_MINECART,
        Material.FURNACE_MINECART,
        Material.TNT_MINECART,
        Material.COMMAND_BLOCK_MINECART,
    )
}
