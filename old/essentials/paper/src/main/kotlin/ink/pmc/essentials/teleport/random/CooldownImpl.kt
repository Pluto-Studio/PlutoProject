package ink.pmc.essentials.teleport.random

import ink.pmc.essentials.api.teleport.random.Cooldown
import ink.pmc.framework.data.getValue
import ink.pmc.framework.data.setValue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class CooldownImpl(override val duration: Duration, private val finishCallback: () -> Unit) : Cooldown, CoroutineScope {
    private var passedSeconds by MutableStateFlow(0)
    override val coroutineContext: CoroutineContext = Dispatchers.Default
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