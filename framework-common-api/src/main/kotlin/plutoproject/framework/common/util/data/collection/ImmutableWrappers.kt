package plutoproject.framework.common.util.data.collection

class ImmutableCollectionWrapper<T>(inner: Collection<T>) : Collection<T> by inner

class ImmutableListWrapper<T>(inner: List<T>) : List<T> by inner

class ImmutableSetWrapper<T>(inner: Set<T>) : Set<T> by inner

fun <T> MutableCollection<T>.toImmutable(): Collection<T> {
    return ImmutableCollectionWrapper(this)
}

fun <T> MutableList<T>.toImmutable(): List<T> {
    return ImmutableListWrapper(this)
}

fun <T> MutableSet<T>.toImmutable(): Set<T> {
    return ImmutableSetWrapper(this)
}
