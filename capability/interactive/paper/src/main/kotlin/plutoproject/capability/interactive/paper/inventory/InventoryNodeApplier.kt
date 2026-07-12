package plutoproject.capability.interactive.paper.inventory

import androidx.compose.runtime.AbstractApplier
import plutoproject.capability.interactive.api.node.InventoryNode

class InventoryNodeApplier(
    root: InventoryNode,
    private val endChangesCallback: () -> Unit,
) : AbstractApplier<InventoryNode>(root) {
    override fun onEndChanges() = endChangesCallback()

    override fun insertBottomUp(index: Int, instance: InventoryNode) {
        current.children.add(index, instance)
        check(instance.parent == null) { "$instance must not have a parent when being inserted." }
        instance.parent = current
    }

    override fun insertTopDown(index: Int, instance: InventoryNode) {
        // Nodes are inserted from the bottom up.
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.children.move(from, to, count)
    }

    override fun onClear() {
        // Keep nodes alive until Composition finishes to avoid concurrent modification during clicks.
    }

    override fun remove(index: Int, count: Int) {
        current.children.remove(index, count)
    }
}
