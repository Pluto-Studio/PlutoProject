package plutoproject.framework.common.util.data.collection

fun <T> Iterable<T>.safeSubList(fromIndex: Int, toIndex: Int): List<T> {
    require(fromIndex in 0..toIndex) { "Invalid indices" }
    require(fromIndex <= toIndex) { "fromIndex must less than toIndex" }
    val list = toList()
    val size = list.size
    if (fromIndex > size) return emptyList()
    if (fromIndex == toIndex) return emptyList()
    val endIndex = if (toIndex > size) size else toIndex
    return list.subList(fromIndex, endIndex)
}
