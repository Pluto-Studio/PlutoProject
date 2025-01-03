package plutoproject.framework.common.util.data.map

import com.google.common.collect.Multimap

operator fun <K, V> Multimap<K, V>.set(key: K, value: V) {
    put(key, value)
}

operator fun <K, V> Multimap<K, V>.set(key: K, value: Iterable<V>) {
    putAll(key, value)
}
