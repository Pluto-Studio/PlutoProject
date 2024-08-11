package ink.pmc.interactive.inventory

import androidx.compose.runtime.AbstractApplier
import ink.pmc.interactive.api.inventory.layout.InventoryNode

class InventoryNodeApplier(root: InventoryNode, private val endChangesCallback: () -> Unit) :
    AbstractApplier<InventoryNode>(root) {

    override fun onEndChanges() {
        endChangesCallback()
    }

    override fun insertBottomUp(index: Int, instance: InventoryNode) {
        current.children.add(index, instance)
        check(instance.parent == null) {
            "$instance must not have a parent when being inserted."
        }
        instance.parent = current
    }

    override fun insertTopDown(index: Int, instance: InventoryNode) {
        // 只从下向上插入
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.children.move(from, to, count)
    }

    override fun onClear() {
        current.children.clear()
    }

    override fun remove(index: Int, count: Int) {
        current.children.remove(index, count)
    }

}