package plutoproject.feature.gallery.core.usecase

import plutoproject.feature.gallery.core.render.DefaultAnimatedImageRenderer
import plutoproject.feature.gallery.core.render.DefaultStaticImageRenderer

fun newRenderStaticImageUseCase(): RenderStaticImageUseCase =
    RenderStaticImageUseCase(DefaultStaticImageRenderer())

fun newRenderAnimatedImageUseCase(): RenderAnimatedImageUseCase =
    RenderAnimatedImageUseCase(DefaultAnimatedImageRenderer())
