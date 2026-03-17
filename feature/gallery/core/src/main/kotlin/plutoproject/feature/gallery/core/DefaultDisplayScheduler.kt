package plutoproject.feature.gallery.core

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

class DefaultDisplayScheduler(
    private val clock: Clock,

    /**
     * 运行调度器自身任务和 [DisplayJob] 唤醒任务的 [CoroutineScope]。
     */
    private val coroutineScope: CoroutineScope,

    /**
     * 调度器自身任务的执行上下文。
     */
    private val schedulerContext: CoroutineContext,

    /**
     * [DisplayJob] 唤醒任务的执行上下文。
     */
    private val awakeContext: CoroutineContext
) : DisplayScheduler {
    override var state: SchedulerState = SchedulerState.IDLING
        private set

    private val lock = Any()
    private val schedules = ConcurrentHashMap<DisplayJob, Instant>()
    private val cleanedUp = AtomicBoolean(false)
    private val stoppedExplicitly = AtomicBoolean(false)
    private val rescheduleSignal = Channel<Unit>(Channel.CONFLATED)

    private var schedulerLoop: Job? = null

    private fun startLoop(): Job {
        return coroutineScope.launch(schedulerContext) {
            runSchedulerLoop()
        }.also { job ->
            job.invokeOnCompletion { cause ->
                handleLoopCompletion(job, cause)
            }
        }
    }

    private suspend fun runSchedulerLoop() {
        currentCoroutineContext()[Job]
            ?: error("Scheduler loop is missing its Job")

        while (true) {
            when (val action = computeLoopAction()) {
                LoopAction.Exit -> return
                is LoopAction.Wait -> awaitNextSignal(action.awakeAt)
                is LoopAction.Wake -> dispatchDueJobs(action.jobs)
            }
        }
    }

    private fun computeLoopAction(): LoopAction = synchronized(lock) {
        if (state == SchedulerState.STOPPED) {
            return@synchronized LoopAction.Exit
        }

        if (schedules.isEmpty()) {
            schedulerLoop = null
            state = SchedulerState.IDLING
            return@synchronized LoopAction.Exit
        }

        val now = Instant.now(clock)
        val dueJobs = collectDueJobs(now)
        if (dueJobs.isNotEmpty()) {
            return@synchronized LoopAction.Wake(dueJobs)
        }

        LoopAction.Wait(schedules.values.minOrNull()!!)
    }

    /**
     * 调用方必须已经持有 [lock]。
     */
    private fun collectDueJobs(now: Instant): List<DisplayJob> {
        val dueJobs = mutableListOf<DisplayJob>()

        for ((job, awakeAt) in schedules) {
            if (!awakeAt.isAfter(now)) {
                dueJobs += job
            }
        }

        dueJobs.forEach(schedules::remove)
        return dueJobs
    }

    private suspend fun awaitNextSignal(awakeAt: Instant) {
        val waitMillis = Duration.between(Instant.now(clock), awakeAt).toMillis()
            .coerceAtLeast(0)

        if (waitMillis == 0L) {
            return
        }

        withTimeoutOrNull(waitMillis) {
            rescheduleSignal.receive()
        }
    }

    private fun dispatchDueJobs(jobs: List<DisplayJob>) {
        jobs.forEach { job ->
            coroutineScope.launch(awakeContext) {
                wakeScheduledJob(job)
            }
        }
    }

    private fun wakeScheduledJob(job: DisplayJob) {
        job.wake()
    }

    private fun handleLoopCompletion(job: Job, cause: Throwable?) {
        val shouldCleanup = synchronized(lock) {
            if (schedulerLoop === job) {
                schedulerLoop = null
            }

            if (state != SchedulerState.STOPPED && schedules.isEmpty()) {
                state = SchedulerState.IDLING
            }

            if (stoppedExplicitly.get()) {
                return@synchronized false
            }

            if (cause == null || cause is InternalLoopCancellation) {
                return@synchronized false
            }

            state = SchedulerState.STOPPED
            true
        }

        if (shouldCleanup) {
            runPostCleanupOnce()
        }
    }

    /**
     * 调用方必须已经持有 [lock]。
     */
    private fun ensureLoopRunning() {
        if (state == SchedulerState.STOPPED || schedules.isEmpty()) {
            return
        }

        if (schedulerLoop?.isActive == true) {
            state = SchedulerState.RUNNING
            return
        }

        schedulerLoop = startLoop()
        state = SchedulerState.RUNNING
    }

    /**
     * 调用方必须已经持有 [lock]。
     */
    private fun moveToIdle(job: Job? = schedulerLoop) {
        if (state == SchedulerState.STOPPED) {
            return
        }

        if (schedulerLoop === job) {
            schedulerLoop = null
        }

        state = SchedulerState.IDLING
        job?.cancel(InternalLoopCancellation("Scheduler loop moved to idle"))
    }

    /**
     * 调用方必须已经持有 [lock]。
     */
    private fun stopScheduler() {
        val loop = schedulerLoop
        schedulerLoop = null
        state = SchedulerState.STOPPED
        loop?.cancel(InternalLoopCancellation("Scheduler loop stopped explicitly"))
    }

    private fun runPostCleanupOnce() {
        if (!cleanedUp.compareAndSet(false, true)) {
            return
        }

        schedules.clear()
    }

    override fun scheduleAwakeAt(job: DisplayJob, awakeAt: Instant) {
        var shouldNotify = false

        synchronized(lock) {
            if (state == SchedulerState.STOPPED) {
                throw IllegalStateException("DisplayScheduler is stopped")
            }

            cleanedUp.set(false)
            schedules[job] = awakeAt

            if (state == SchedulerState.IDLING) {
                ensureLoopRunning()
                return@synchronized
            }

            shouldNotify = true
        }

        if (shouldNotify) {
            rescheduleSignal.trySend(Unit)
        }
    }

    override fun unschedule(job: DisplayJob) {
        var shouldNotify = false

        synchronized(lock) {
            schedules.remove(job)

            if (state == SchedulerState.STOPPED) {
                return
            }

            if (schedules.isEmpty()) {
                moveToIdle()
                return
            }

            if (state == SchedulerState.RUNNING) {
                shouldNotify = true
            }
        }

        if (shouldNotify) {
            rescheduleSignal.trySend(Unit)
        }
    }

    override fun stop() {
        synchronized(lock) {
            if (state == SchedulerState.STOPPED) {
                return
            }

            stoppedExplicitly.set(true)
            stopScheduler()
        }

        runPostCleanupOnce()
    }

    private sealed interface LoopAction {
        data object Exit : LoopAction

        data class Wait(val awakeAt: Instant) : LoopAction

        data class Wake(val jobs: List<DisplayJob>) : LoopAction
    }

    private class InternalLoopCancellation(message: String) : CancellationException(message)
}
