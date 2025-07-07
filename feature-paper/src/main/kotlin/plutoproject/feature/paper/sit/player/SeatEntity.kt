package plutoproject.feature.paper.sit.player

import net.minecraft.world.entity.AreaEffectCloud
import org.bukkit.Location
import org.bukkit.craftbukkit.CraftWorld

class SeatEntity(location: Location) : AreaEffectCloud(
    (location.world as CraftWorld).handle,
    location.x,
    location.y,
    location.z
) {
    init {
        persist = false
        radius = 0f
        duration = -1
        isNoGravity = true
        isInvisible = true
        isInvulnerable = true
    }

    override fun tick() {}

    override fun handlePortal() {}

    override fun dismountsUnderwater(): Boolean {
        return false
    }
}
