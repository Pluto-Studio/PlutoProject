package plutoproject.framework.common.util.coroutine

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

private typealias SuspendAction<T> = suspend () -> T

private class ConfinementTask(
    val action: SuspendAction<Any?>,
    val resultReceiver: CompletableDeferred<Any?>,
)

private object ConfinementContextMarker : CoroutineContext.Element, CoroutineContext.Key<ConfinementContextMarker> {
    override val key: CoroutineContext.Key<ConfinementContextMarker> = this
}

/**
 * 线程封闭的任务执行器，用于串行且同步地执行任务，因此可以代替 [Mutex] 或其他锁机制来实现线程安全。
 *
 * 该执行器是可重入的，意味着可以在执行的代码中再次调用执行函数而不会像 [Mutex] 一样死锁。
 *
 * @param coroutineScope 依赖的作用域，当这个作用域被取消时候执行器会一并被取消
 * @param coroutineContext 执行任务时使用的上下文，默认从依赖的作用域继承
 */
@Suppress("UNCHECKED_CAST")
class ConfinementExecutor(
    coroutineScope: CoroutineScope,
    coroutineContext: CoroutineContext = coroutineScope.coroutineContext
) {
    @Volatile
    private var _isActive = true
    val isActive: Boolean
        get() = _isActive

    private val lifecycleScope = createLifecycleScope(coroutineScope, coroutineContext)
    private val confinementContext = coroutineContext.createSingleParallelismContext() + ConfinementContextMarker
    private val channel = Channel<ConfinementTask>(Channel.UNLIMITED)

    init {
        lifecycleScope.launch(confinementContext) {
            for (task in channel) {
                executeTask(task)
            }
        }
    }

    private suspend fun executeTask(task: ConfinementTask) {
        try {
            task.resultReceiver.complete(task.action())
        } catch (e: CancellationException) {
            task.resultReceiver.cancel(e)
            throw e
        } catch (e: Throwable) {
            task.resultReceiver.completeExceptionally(e)
        }
    }

    private fun createLifecycleScope(
        parentScope: CoroutineScope,
        parentContext: CoroutineContext
    ): CoroutineScope {
        return CoroutineScope(parentContext + SupervisorJob(findParentJob(parentScope, parentContext)))
    }

    private fun findParentJob(coroutineScope: CoroutineScope, coroutineContext: CoroutineContext): Job {
        return coroutineContext[Job] ?: coroutineScope.coroutineContext[Job] ?: error("Unable to find a parent job")
    }

    private fun CoroutineContext.createSingleParallelismContext(): CoroutineContext {
        val dispatcher = this[ContinuationInterceptor] as? CoroutineDispatcher ?: Dispatchers.Main
        return dispatcher.limitedParallelism(1)
    }

    private val CoroutineContext.isConfinementContext: Boolean
        get() = this[ConfinementContextMarker] != null

    /**
     * 执行一个任务，对该函数的调用会保持挂起直到 [action] 执行完毕。
     *
     * 若 [action] 中抛出了任何异常，也会传递到该函数的调用处。
     *
     * @param action 要执行的任务
     * @return 任务返回的结果
     */
    suspend fun <T> execute(action: SuspendAction<T>): T {
        check(isActive) { "This ConfinementExecutor instance is not active" }
        if (coroutineContext.isConfinementContext) {
            return action()
        }
        val resultReceiver = CompletableDeferred<Any?>()
        channel.send(ConfinementTask(action, resultReceiver))
        return resultReceiver.await() as T
    }

    /**
     * 异步执行一个任务，该函数不会阻塞或挂起。
     *
     * @param action 要执行的任务
     * @return 开启的异步任务，可以通过 [Deferred.await] 等待并获取值
     */
    fun <T> executeAsync(action: SuspendAction<T>): Deferred<T> {
        check(isActive) { "This ConfinementExecutor instance is not active" }
        return lifecycleScope.async { execute(action) }
    }

    /**
     * 关闭这个 [ConfinementExecutor]。
     */
    fun cancel() {
        if (!isActive) return
        channel.cancel()
        lifecycleScope.coroutineContext[Job]?.cancel()
        _isActive = false
    }

    /**
     * 关闭这个 [ConfinementExecutor] 并等待完成。
     */
    suspend fun cancelAndJoin() {
        if (!isActive) return
        withContext(NonCancellable) {
            channel.cancel()
            lifecycleScope.coroutineContext[Job]?.cancelAndJoin()
            this@ConfinementExecutor._isActive = false
        }
    }
}

/**
 * 依赖当前 [CoroutineScope] 创建一个 [ConfinementExecutor]。
 *
 * @param coroutineContext 执行任务时使用的上下文，默认从依赖的作用域继承
 */
fun CoroutineScope.ConfinementExecutor(coroutineContext: CoroutineContext = this.coroutineContext): ConfinementExecutor {
    return ConfinementExecutor(this, coroutineContext)
}
