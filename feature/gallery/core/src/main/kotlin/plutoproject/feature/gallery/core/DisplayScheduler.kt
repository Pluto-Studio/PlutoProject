package plutoproject.feature.gallery.core

import java.time.Instant

interface DisplayScheduler {
    val state: SchedulerState

    fun scheduleAwakeAt(job: DisplayJob, awakeAt: Instant)

    fun unschedule(job: DisplayJob)

    fun stop()
}


enum class SchedulerState {
    /**
     * 正在工作，并且有至少一个要处理的 DisplayJob。
     */
    RUNNING,

    /**
     * 正在工作，但是没有要处理的 DisplayJob，处于空闲状态。
     */
    IDLING,

    /**
     * 已清理并停止。
     */
    STOPPED
}
