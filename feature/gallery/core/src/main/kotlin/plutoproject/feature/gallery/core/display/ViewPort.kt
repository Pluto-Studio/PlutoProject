package plutoproject.feature.gallery.core.display

interface ViewPort {
    fun getPlayerViews(world: String): List<PlayerView>
}
