package plutoproject.framework.common.util.data.map

import java.util.concurrent.ConcurrentHashMap

/**
 * 创建一个线程安全的 [MutableMap] 对象。
 *
 * @param K 键类型
 * @param V 值类型
 * @return 被创建的 [MutableMap] 对象
 */
fun <K, V> mutableConcurrentMapOf(): MutableMap<K, V> {
    return ConcurrentHashMap()
}
