package plutoproject.capability.interactive.api.click

import plutoproject.capability.interactive.api.drag.DragScope

interface ClickHandler {
    suspend fun processClick(scope: ClickScope): ClickResult
    suspend fun processDrag(scope: DragScope)
}
