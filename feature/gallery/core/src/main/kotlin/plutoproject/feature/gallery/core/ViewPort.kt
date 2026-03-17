package plutoproject.feature.gallery.core

interface ViewPort {
    fun getPlayerViews(world: String): List<PlayerView>
}
