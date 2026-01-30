package plutoproject.framework.common.feature.dependency

/**
 * 拓扑排序工具类，用于计算依赖加载/禁用顺序
 */
object TopologicalSort {
    /**
     * 执行拓扑排序
     * @param nodes 所有节点 ID
     * @param edges 边关系 (from -> to 表示 from 依赖 to，to 需要在 beforeLoading 时先加载)
     * @param beforeEdges 需要在目标之前处理的边
     * @param afterEdges 需要在目标之后处理的边
     * @return 排序后的节点 ID 列表，如果存在循环依赖则返回空列表
     */
    fun sort(
        nodes: Set<String>,
        edges: Map<String, Set<String>>,
        beforeEdges: Map<String, Set<String>> = emptyMap(),
        afterEdges: Map<String, Set<String>> = emptyMap()
    ): List<String> {
        // 计算入度
        val inDegree = mutableMapOf<String, Int>()
        nodes.forEach { inDegree[it] = 0 }
        
        // 合并所有边
        val allEdges = mutableMapOf<String, MutableSet<String>>()
        nodes.forEach { allEdges[it] = mutableSetOf() }
        
        // 处理普通依赖边 (A 依赖 B，B 需要先加载)
        edges.forEach { (from, tos) ->
            tos.forEach { to ->
                if (to in nodes) {
                    allEdges.getOrPut(to) { mutableSetOf() }.add(from)
                    inDegree[from] = inDegree.getOrDefault(from, 0) + 1
                }
            }
        }
        
        // 处理 BEFORE 边 (A 的 BEFORE 依赖 B，B 需要先加载)
        beforeEdges.forEach { (from, tos) ->
            tos.forEach { to ->
                if (to in nodes) {
                    allEdges.getOrPut(to) { mutableSetOf() }.add(from)
                    inDegree[from] = inDegree.getOrDefault(from, 0) + 1
                }
            }
        }
        
        // 处理 AFTER 边 (A 的 AFTER 依赖 B，A 需要先加载，B 后加载)
        afterEdges.forEach { (from, tos) ->
            tos.forEach { to ->
                if (to in nodes) {
                    allEdges.getOrPut(from) { mutableSetOf() }.add(to)
                    inDegree[to] = inDegree.getOrDefault(to, 0) + 1
                }
            }
        }
        
        // Kahn 算法
        val queue = ArrayDeque<String>()
        val result = mutableListOf<String>()
        
        inDegree.filter { it.value == 0 }.keys.forEach { queue.add(it) }
        
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            result.add(current)
            
            allEdges[current]?.forEach { neighbor ->
                val newDegree = inDegree.getOrDefault(neighbor, 0) - 1
                inDegree[neighbor] = newDegree
                if (newDegree == 0) {
                    queue.add(neighbor)
                }
            }
        }
        
        // 如果结果数量不等于节点数量，说明存在循环依赖
        return if (result.size == nodes.size) result else emptyList()
    }
    
    /**
     * 检测图中的所有循环
     * @param nodes 所有节点
     * @param edges 边关系
     * @return 所有检测到的循环路径列表
     */
    fun detectCycles(
        nodes: Set<String>,
        edges: Map<String, Set<String>>
    ): List<List<String>> {
        val cycles = mutableListOf<List<String>>()
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()
        val path = mutableListOf<String>()
        
        fun dfs(node: String) {
            if (node in recursionStack) {
                // 找到循环
                val cycleStart = path.indexOf(node)
                if (cycleStart >= 0) {
                    val cycle = path.subList(cycleStart, path.size) + node
                    cycles.add(cycle.toList())
                }
                return
            }
            
            if (node in visited) return
            
            visited.add(node)
            recursionStack.add(node)
            path.add(node)
            
            edges[node]?.forEach { neighbor ->
                if (neighbor in nodes) {
                    dfs(neighbor)
                }
            }
            
            path.removeAt(path.size - 1)
            recursionStack.remove(node)
        }
        
        nodes.forEach { node ->
            if (node !in visited) {
                visited.clear()
                recursionStack.clear()
                path.clear()
                dfs(node)
            }
        }
        
        return cycles.distinct()
    }
    
    /**
     * 获取反向拓扑排序（用于禁用操作，被依赖的先禁用）
     */
    fun reverseSort(
        nodes: Set<String>,
        edges: Map<String, Set<String>>,
        beforeEdges: Map<String, Set<String>> = emptyMap(),
        afterEdges: Map<String, Set<String>> = emptyMap()
    ): List<String> {
        return sort(nodes, edges, beforeEdges, afterEdges).reversed()
    }
}
