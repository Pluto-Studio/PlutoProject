package plutoproject.framework.common.util.data.collection

import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun <T> mutableConcurrentListOf(vararg elements: T): MutableList<T> {
    return Collections.synchronizedList(mutableListOf(*elements))
}

fun <T> mutableConcurrentSetOf(vararg elements: T): MutableSet<T> {
    return ConcurrentHashMap.newKeySet()
}
