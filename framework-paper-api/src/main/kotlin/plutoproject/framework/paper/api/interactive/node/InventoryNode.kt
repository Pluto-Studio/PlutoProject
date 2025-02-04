package plutoproject.framework.paper.api.interactive.node

import org.bukkit.inventory.ItemStack
import plutoproject.framework.paper.api.interactive.canvas.Canvas
import plutoproject.framework.paper.api.interactive.canvas.OffsetCanvas
import plutoproject.framework.paper.api.interactive.click.ClickModifier
import plutoproject.framework.paper.api.interactive.click.ClickResult
import plutoproject.framework.paper.api.interactive.click.ClickScope
import plutoproject.framework.paper.api.interactive.drag.DragModifier
import plutoproject.framework.paper.api.interactive.drag.DragScope
import plutoproject.framework.paper.api.interactive.layout.Constraints
import plutoproject.framework.paper.api.interactive.measuring.*
import plutoproject.framework.paper.api.interactive.modifiers.LayoutChangingModifier
import plutoproject.framework.paper.api.interactive.modifiers.Modifier
import plutoproject.framework.paper.api.interactive.modifiers.OnSizeChangedModifier
import plutoproject.framework.paper.api.interactive.util.*
import kotlin.reflect.KClass

/**
 * TODO structure is really not decided on yet.
 *  I'd really like to avoid inheritance, and have only one ComposableNode call that creates Layout.
 *  You can configure stuff through [measurePolicy], [placer], and the [modifier], but things creates some problems
 *  when trying to make your own composable nodes that interact with this Layout builder.
 */
class InventoryNode : Measurable, Placeable, BaseInventoryNode {
    override var measurePolicy: MeasurePolicy = ChildMeasurePolicy
    override var renderer: Renderer = EmptyRenderer
    override var canvas: Canvas? = null

    val children = mutableListOf<InventoryNode>()
    override var modifier: Modifier = Modifier.Companion
        set(value) {
            field = value
            processedModifier = modifier.foldIn(mutableMapOf()) { acc, element ->
                val existing = acc[element::class]
                if (existing != null) {
                    acc[element::class] = existing.unsafeMergeWith(element)
                } else {
                    acc[element::class] = element
                }
                acc
            }
            layoutChangingModifiers = modifier.foldIn(mutableListOf()) { acc, element ->
                if (element is LayoutChangingModifier) acc.add(element)
                acc
            }
        }

    var processedModifier = mapOf<KClass<out Modifier.Element<*>>, Modifier.Element<*>>()
    var layoutChangingModifiers: List<LayoutChangingModifier> = emptyList()

    inline fun <reified T : Modifier.Element<T>> get(): T? {
        return processedModifier[T::class] as T?
    }

    var parent: InventoryNode? = null

    override var width: Int = 0
    override var height: Int = 0
    override var x: Int = 0
    override var y: Int = 0

    private fun coercedConstraints(constraints: Constraints) = with(constraints) {
        object : Placeable by this@InventoryNode {
            override var width: Int = this@InventoryNode.width.coerceIn(minWidth..maxWidth)
            override var height: Int = this@InventoryNode.height.coerceIn(minHeight..maxHeight)
        }
    }

    override fun measure(constraints: Constraints): Placeable {
        val innerConstraints = layoutChangingModifiers.fold(constraints) { inner, modifier ->
            modifier.modifyInnerConstraints(inner)
        }
        val result = measurePolicy.measure(children, innerConstraints)

        if (width != result.width || height != result.height) {
            get<OnSizeChangedModifier>()?.onSizeChanged?.invoke(Size(result.width, result.height))
        }
        width = result.width
        height = result.height
        result.placer.placeChildren()

        val layoutConstraints = layoutChangingModifiers.fold(constraints) { outer, modifier ->
            modifier.modifyLayoutConstraints(IntSize(result.width, result.height), outer)
        }
        // Returned constraints will always appear as though they are in parent's bounds
        return coercedConstraints(layoutConstraints)
    }

    override fun placeAt(x: Int, y: Int) {
        val offset = layoutChangingModifiers.fold(IntOffset(x, y)) { acc, modifier ->
            modifier.modifyPosition(acc)
        }
        this.x = offset.x
        this.y = offset.y
    }

    override fun renderTo(canvas: Canvas?) {
        val offsetCanvas = (canvas ?: this.canvas)?.let { OffsetCanvas(x, y, it) }
        renderer.apply { offsetCanvas?.render(this@InventoryNode) }
        for (child in children) child.renderTo(offsetCanvas)
        renderer.apply { offsetCanvas?.renderAfterChildren(this@InventoryNode) }
    }

    /**
     * @return Whether no elements were clickable or any element requested the bukkit click event to be cancelled.
     */
    suspend fun processClick(scope: ClickScope, x: Int, y: Int): ClickResult {
        val cancelClickEvent = get<ClickModifier>()?.run {
            onClick(scope)
            cancelClickEvent
        }
        return children
            .filter { x in it.x until (it.x + it.width) && y in it.y until (it.y + it.height) }
            .fold(ClickResult(cancelClickEvent)) { acc, it ->
                acc.mergeWith(it.processClick(scope, x - it.x, y - it.y))
            }
    }

    data class DragInfo(
        val dragModifier: DragModifier,
        val itemMap: ItemPositions = ItemPositions(),
    )

    fun buildDragMap(
        coords: IntCoordinates,
        item: ItemStack,
        dragMap: MutableMap<BaseInventoryNode, DragInfo>
    ): Boolean {
        val (iX, iY) = coords
        if (iX !in 0 until width || iY !in 0 until height) return false
        val dragModifier = get<DragModifier>()
        if (dragModifier != null) {
            val drag = dragMap.getOrPut(this) { DragInfo(dragModifier) }
            dragMap[this] = drag.copy(itemMap = drag.itemMap.plus(iX, iY, item))
            return true
        }
        return children.any { child ->
            //TODO ensure this offset is still correct in x,z
            val newCoords = IntCoordinates(iX - x + child.x, iY - x + child.x)
            child.buildDragMap(newCoords, item, dragMap)
        }
    }

    fun processDrag(scope: DragScope) {
        val dragMap = mutableMapOf<BaseInventoryNode, DragInfo>()
        scope.updatedItems.items.forEach { (coords, item) ->
            buildDragMap(coords, item, dragMap)
        }
        dragMap.forEach { (_, info) ->
            info.dragModifier.onDrag.invoke(scope.copy(updatedItems = info.itemMap))
        }
    }

    override fun toString() = children.joinToString(prefix = "LayoutNode(", postfix = ")")

    internal companion object {
        val ChildMeasurePolicy = MeasurePolicy { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            MeasureResult(placeables.maxOfOrNull { it.width } ?: 0, placeables.maxOfOrNull { it.height } ?: 0) {
                placeables.forEach { it.placeAt(0, 0) }
            }
        }
        private val ErrorMeasurePolicy = MeasurePolicy { _, _ -> error("Measurer not defined") }
    }
}

val EmptyRenderer = object : Renderer {}
