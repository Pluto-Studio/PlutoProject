package plutoproject.feature.gallery.core

data class AllocationRange(
    val start: Int,
    val end: Int,
) {
    init {
        check(start <= end) { "allocationRange start must be <= end: start=$start, end=$end" }
    }
}
