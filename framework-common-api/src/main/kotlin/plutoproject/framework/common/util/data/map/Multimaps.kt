package plutoproject.framework.common.util.data.map

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps

fun <K, V> listMultimapOf(): Multimap<K, V> {
    return ArrayListMultimap.create()
}

fun <K, V> concurrentListMultimapOf(): Multimap<K, V> {
    return Multimaps.synchronizedMultimap(listMultimapOf())
}
