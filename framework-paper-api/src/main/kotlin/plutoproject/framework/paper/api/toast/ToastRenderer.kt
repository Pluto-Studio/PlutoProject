package plutoproject.framework.paper.api.toast

interface ToastRenderer<P> : GenericRenderer<P, Toast> {
    override fun render(player: P, obj: Toast)
}
