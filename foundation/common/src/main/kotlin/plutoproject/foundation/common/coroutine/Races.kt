package plutoproject.foundation.common.coroutine

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.selects.select

@JvmName("raceIterableJob")
suspend fun race(jobs: Iterable<Job>) = select<Unit> {
    jobs.forEach { it.onJoin }
}

suspend fun race(vararg jobs: Job) = race(jobs.toList())

@JvmName("raceIterableDeferred")
suspend fun <T> race(jobs: Iterable<Deferred<T>>): T = select {
    jobs.forEach {
        it.onAwait { result -> result }
    }
}

suspend fun <T> race(vararg jobs: Deferred<T>): T = race(jobs.toList())

suspend fun <T> raceConditional(jobs: Iterable<Deferred<T?>>, condition: (T?) -> Boolean): T? {
    val remaining = jobs.toMutableList()

    try {
        while (remaining.isNotEmpty()) {
            val selected = select {
                remaining.forEach { deferred ->
                    deferred.onAwait { result ->
                        if (condition(result)) {
                            remaining.forEach { it.cancel() }
                            result
                        } else {
                            remaining.remove(deferred)
                            null
                        }
                    }
                }
            }

            if (selected != null) {
                return selected
            }
        }
    } finally {
        // 确保所有剩余的任务在函数返回之前被取消
        remaining.forEach { it.cancel() }
    }

    return null
}

suspend fun <T> raceConditional(vararg tasks: Deferred<T?>, condition: (T?) -> Boolean): T? {
    return raceConditional(tasks.toMutableList(), condition)
}
