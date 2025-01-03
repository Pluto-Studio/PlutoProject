package plutoproject.framework.paper.api.toast

interface GenericRenderer<P, T> {
    fun render(player: P, obj: T)
}
