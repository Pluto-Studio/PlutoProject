package plutoproject.framework.common.util.data

import java.util.*

@Suppress("UNCHECKED_CAST")
fun <T> optionalOf(obj: T?): Optional<T> = Optional.ofNullable(obj) as Optional<T>

@Suppress("UNCHECKED_CAST")
fun <T> emptyOptionalOf(): Optional<T> = Optional.empty<T>() as Optional<T>

val EmptyOptional: Optional<Nothing> = Optional.empty()
