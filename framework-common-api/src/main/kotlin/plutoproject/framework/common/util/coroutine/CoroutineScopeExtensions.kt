package plutoproject.framework.common.util.coroutine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job

fun CoroutineScope.createSupervisorChild(): CoroutineScope {
    return CoroutineScope(coroutineContext + SupervisorJob(coroutineContext.job))
}
