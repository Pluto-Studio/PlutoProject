package ink.pmc.framework.visual.display.text

import ink.pmc.framework.utils.inject.inlinedGet
import org.koin.core.qualifier.named

object DefaultTextDisplayRenderer : TextDisplayRenderer by inlinedGet(named("internal"))