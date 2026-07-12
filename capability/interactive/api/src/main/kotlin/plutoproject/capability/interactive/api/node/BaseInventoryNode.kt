package plutoproject.capability.interactive.api.node

import plutoproject.capability.interactive.api.canvas.Canvas
import plutoproject.capability.interactive.api.measuring.MeasurePolicy
import plutoproject.capability.interactive.api.modifiers.Modifier
import plutoproject.capability.interactive.api.measuring.Renderer

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
