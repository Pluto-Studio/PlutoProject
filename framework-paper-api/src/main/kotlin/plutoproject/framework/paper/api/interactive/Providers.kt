package plutoproject.framework.paper.api.interactive

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import plutoproject.framework.paper.api.interactive.canvas.Canvas
import plutoproject.framework.paper.api.interactive.click.ClickHandler

val LocalClickHandler: ProvidableCompositionLocal<ClickHandler> =
    staticCompositionLocalOf { error("No provider for local click handler") }
val LocalCanvas: ProvidableCompositionLocal<Canvas?> =
    staticCompositionLocalOf { null }
