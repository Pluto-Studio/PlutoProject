package plutoproject.framework.common.util.coroutine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

private val loomExecutor = Executors.newVirtualThreadPerTaskExecutor()
private val loomDispatcher = loomExecutor.asCoroutineDispatcher()

/**
 * 基于虚拟线程的 [CoroutineDispatcher]，适合用于 IO 任务。
 */
@Suppress("UnusedReceiverParameter")
val Dispatchers.Loom: CoroutineDispatcher
    get() = loomDispatcher

internal fun shutdownLoomDispatcher() {
    loomDispatcher.close()
}
