package plutoproject.framework.common.bridge.exception

import plutoproject.framework.common.api.bridge.ResultWrapper

interface StatefulResultWrapper<T, S> : ResultWrapper<T> {
    override val valueOrThrow: T
        get() = if (isExceptionOccurred) throwException() else value!!
    val state: S
    val isExceptionOccurred: Boolean

    fun throwException(): Nothing
}
