package plutoproject.feature.gallery.adapter.paper

import org.bukkit.Bukkit
import plutoproject.feature.gallery.core.display.PlayerView
import plutoproject.feature.gallery.core.display.Vec3
import plutoproject.feature.gallery.core.display.ViewPort

class PaperViewPort : ViewPort {
    override fun getPlayerViews(world: String): List<PlayerView> {
        val bukkitWorld = Bukkit.getWorld(world) ?: return emptyList()
        return bukkitWorld.players.map { player ->
            val eye = player.eyeLocation
            val direction = eye.direction

            PlayerView(
                id = player.uniqueId,
                eye = Vec3(eye.x, eye.y, eye.z),
                viewDirection = Vec3(direction.x, direction.y, direction.z),
            )
        }
    }
}
