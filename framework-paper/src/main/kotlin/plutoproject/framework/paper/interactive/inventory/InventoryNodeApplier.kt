package plutoproject.framework.paper.interactive.inventory

import androidx.compose.runtime.AbstractApplier
import plutoproject.framework.paper.api.interactive.node.InventoryNode

class InventoryNodeApplier(
    root: InventoryNode,
    private val endChangesCallback: () -> Unit
) : AbstractApplier<InventoryNode>(root) {
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
        /*
         关闭菜单时，Compose 会使用此处的逻辑进行清理。
         若是因点击事件关闭菜单，在遍历子节点处理点击事件时，Compose 也会异步清理掉所有子节点，
         从而引发 ConcurrentModificationException。
         此处的解决方法是不清理节点列表，等 Composition 退出后会自己被回收。
        */
    }

    override fun remove(index: Int, count: Int) {
        current.children.remove(index, count)
    }
}
