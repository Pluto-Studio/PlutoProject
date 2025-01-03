package plutoproject.framework.paper.api.interactive.click

import plutoproject.framework.paper.api.interactive.drag.DragScope

interface ClickHandler {
    suspend fun processClick(scope: ClickScope): ClickResult
    suspend fun processDrag(scope: DragScope)
}
