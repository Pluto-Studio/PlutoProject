package plutoproject.framework.paper.api.interactive.node

import plutoproject.framework.paper.api.interactive.canvas.Canvas
import plutoproject.framework.paper.api.interactive.measuring.MeasurePolicy
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.measuring.Renderer

interface BaseInventoryNode {
    var measurePolicy: MeasurePolicy
    var renderer: Renderer
    var canvas: Canvas?
    var modifier: Modifier
    var width: Int
    var height: Int
    var x: Int
    var y: Int

    fun render() = renderTo(null)

    fun renderTo(canvas: Canvas?)

    companion object {
        val Constructor: () -> BaseInventoryNode = ::InventoryNode
    }
}
