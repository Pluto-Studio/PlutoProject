package plutoproject.framework.common.util.coroutine

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

@Suppress("UNUSED")
fun <T> runAsync(
    coroutineScope: CoroutineScope = PlutoCoroutineScope,
    coroutineContext: CoroutineContext = Dispatchers.Default,
    coroutineStart: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T,
): Deferred<T> = coroutineScope.async(coroutineContext, coroutineStart) { block() }

@Suppress("UNUSED")
fun <T> runAsyncIO(
    coroutineScope: CoroutineScope = PlutoCoroutineScope,
    coroutineStart: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T,
): Deferred<T> = coroutineScope.async(Dispatchers.IO, coroutineStart) { block() }

@Suppress("UNUSED")
fun <T> runAsyncUnconfined(
    coroutineScope: CoroutineScope = PlutoCoroutineScope,
    coroutineStart: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T,
): Deferred<T> = coroutineScope.async(Dispatchers.Unconfined, coroutineStart) { block() }

@Suppress("UNUSED")
suspend fun <T> withDefault(block: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.Default) { block() }

@Suppress("UNUSED")
suspend fun <T> withIO(block: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.IO) { block() }

@Suppress("UNUSED")
suspend fun <T> withUnconfined(block: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.Unconfined) { block() }
