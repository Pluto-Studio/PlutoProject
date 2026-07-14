package plutoproject.feature.randomteleport.paper

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import plutoproject.feature.randomteleport.api.paper.Cooldown
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import plutoproject.kernel.api.currentModuleContext
import plutoproject.foundation.common.flow.getValue
import plutoproject.foundation.common.flow.setValue

class CooldownImpl(override val duration: Duration, private val finishCallback: () -> Unit) : Cooldown, CoroutineScope {
    private var passedSeconds by MutableStateFlow(0)
    override val coroutineContext: CoroutineContext =
        currentModuleContext().coroutineScope.coroutineContext +
            SupervisorJob(currentModuleContext().coroutineScope.coroutineContext[Job]) +
            Dispatchers.Default
    override var isFinished: Boolean by MutableStateFlow(false)
    override var remainingSeconds: Long by MutableStateFlow(duration.inWholeSeconds)

    init {
        launch {
            while (true) {
                delay(1.seconds)
                if (isFinished) break
                remainingSeconds = duration.inWholeSeconds - (++passedSeconds)
            }
        }
        launch {
            delay(duration)
            finish()
        }
    }

    override fun finish() {
        isFinished = true
        runCatching {
            cancel()
        }
        finishCallback()
    }
}
