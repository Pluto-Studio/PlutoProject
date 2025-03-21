package plutoproject.framework.common.api.bridge

interface ResultWrapper<T> {
    val value: T?
    val valueOrThrow: T
}
