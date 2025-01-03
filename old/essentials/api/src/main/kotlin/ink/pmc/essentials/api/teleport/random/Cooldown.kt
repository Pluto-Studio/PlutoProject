package ink.pmc.essentials.api.teleport.random

import kotlin.time.Duration

interface Cooldown {
    val isFinished: Boolean
    val duration: Duration
    val remainingSeconds: Long

    fun finish()
}