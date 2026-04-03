package plutoproject.feature.gallery.core.display.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.DisplayManager
import plutoproject.feature.gallery.core.display.DisplayScheduler
import plutoproject.feature.gallery.core.display.ViewPort
import plutoproject.feature.gallery.core.display.job.DisplayJob
import plutoproject.feature.gallery.core.display.job.DisplayJobFactory
import plutoproject.feature.gallery.core.display.job.StaticDisplayJob
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.display.SchedulerState
import plutoproject.feature.gallery.core.dummyUuid
import plutoproject.feature.gallery.core.sampleDisplayInstance
import plutoproject.feature.gallery.core.sampleImage
import plutoproject.feature.gallery.core.sampleStaticImageDataEntry
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class DisplayRuntimeLifecycleUseCaseTest {
    @Test
    fun `start display job should create attach bind and schedule first awake`() {
        val manager = DisplayManager()
        val scheduler = RecordingDisplayScheduler()
        val factory = DisplayJobFactory(
            displayScheduler = scheduler,
            viewPort = object : ViewPort {
                override fun getPlayerViews(world: String) = emptyList<plutoproject.feature.gallery.core.display.PlayerView>()
            },
            displayManager = manager,
            clock = fixedClock(1_000L),
            animatedMaxFramesPerSecond = 20,
            visibleDistance = 5.0,
            staticUpdateInterval = 1.seconds,
        )
        val useCase = StartDisplayJobUseCase(fixedClock(1_000L), scheduler, manager, factory)
        val belongsTo = dummyUuid(7001)
        val displayInstance = sampleDisplayInstance(id = dummyUuid(7002), belongsTo = belongsTo)
        val image = sampleImage(id = belongsTo)
        val entry = sampleStaticImageDataEntry(belongsTo)

        val result = useCase.execute(displayInstance, image, entry)
        val job = (result as StartDisplayJobUseCase.Result.Ok).job

        assertTrue(job is StaticDisplayJob)
        assertSame(job, manager.getLoadedDisplayJob(job.belongsTo))
        assertEquals(job.belongsTo, manager.getJobBelongsToByDisplayInstanceId(displayInstance.id))
        assertSame(displayInstance, job.managedDisplayInstances[displayInstance.id])
        assertSame(job, scheduler.lastScheduledJob)
        assertEquals(Instant.ofEpochMilli(1_000L), scheduler.lastAwakeAt)
    }

    @Test
    fun `start display job should return already started when job already exists`() {
        val manager = DisplayManager()
        val existed = FakeDisplayJob(dummyUuid(7011))
        manager.registerDisplayJob(existed)
        val useCase = StartDisplayJobUseCase(
            fixedClock(0L),
            RecordingDisplayScheduler(),
            manager,
            DisplayJobFactory(
                displayScheduler = RecordingDisplayScheduler(),
                viewPort = object : ViewPort {
                    override fun getPlayerViews(world: String) = emptyList<plutoproject.feature.gallery.core.display.PlayerView>()
                },
                displayManager = manager,
                clock = fixedClock(0L),
                animatedMaxFramesPerSecond = 20,
                visibleDistance = 5.0,
                staticUpdateInterval = 1.seconds,
            )
        )

        val result = useCase.execute(
            sampleDisplayInstance(id = dummyUuid(7013), belongsTo = existed.belongsTo),
            sampleImage(id = existed.belongsTo),
            sampleStaticImageDataEntry(existed.belongsTo),
        )

        assertEquals(StartDisplayJobUseCase.Result.AlreadyStarted(existed), result)
    }

    @Test
    fun `attach and detach display instance should update job binding`() {
        val manager = DisplayManager()
        val job = FakeDisplayJob(dummyUuid(7021))
        manager.registerDisplayJob(job)
        val attach = AttachDisplayInstanceToJobUseCase(manager)
        val detach = DetachDisplayInstanceFromJobUseCase(manager)
        val displayInstance = sampleDisplayInstance(id = dummyUuid(7022), belongsTo = job.belongsTo)

        val attachResult = attach.execute(displayInstance, sampleImage(id = job.belongsTo), sampleStaticImageDataEntry(job.belongsTo))
        assertEquals(AttachDisplayInstanceToJobUseCase.Result.Ok(job), attachResult)
        assertEquals(job.belongsTo, manager.getJobBelongsToByDisplayInstanceId(displayInstance.id))

        val detachResult = detach.execute(displayInstance.id)
        assertEquals(DetachDisplayInstanceFromJobUseCase.Result.Ok(job, displayInstance), detachResult)
        assertNull(manager.getJobBelongsToByDisplayInstanceId(displayInstance.id))
    }

    @Test
    fun `stop display job should unschedule unbind stop and remove`() {
        val manager = DisplayManager()
        val scheduler = RecordingDisplayScheduler()
        val job = FakeDisplayJob(dummyUuid(7031))
        val first = sampleDisplayInstance(id = dummyUuid(7032), belongsTo = job.belongsTo)
        val second = sampleDisplayInstance(id = dummyUuid(7033), belongsTo = job.belongsTo)
        job.attach(first, sampleImage(id = job.belongsTo), sampleStaticImageDataEntry(job.belongsTo))
        job.attach(second, sampleImage(id = job.belongsTo), sampleStaticImageDataEntry(job.belongsTo))
        manager.registerDisplayJob(job)
        manager.bindDisplayInstanceToJob(first.id, job.belongsTo)
        manager.bindDisplayInstanceToJob(second.id, job.belongsTo)

        val result = StopDisplayJobUseCase(scheduler, manager).execute(job.belongsTo)

        assertEquals(StopDisplayJobUseCase.Result.Ok(job), result)
        assertSame(job, scheduler.lastUnscheduledJob)
        assertTrue(job.stopCalled)
        assertNull(manager.getLoadedDisplayJob(job.belongsTo))
        assertNull(manager.getJobBelongsToByDisplayInstanceId(first.id))
        assertNull(manager.getJobBelongsToByDisplayInstanceId(second.id))
    }

    @Test
    fun `attach and detach should return not started when runtime job is absent`() {
        val manager = DisplayManager()
        val displayInstance = sampleDisplayInstance(id = dummyUuid(7041), belongsTo = dummyUuid(7042))

        assertEquals(
            AttachDisplayInstanceToJobUseCase.Result.JobNotStarted,
            AttachDisplayInstanceToJobUseCase(manager).execute(displayInstance, sampleImage(id = displayInstance.belongsTo), sampleStaticImageDataEntry(displayInstance.belongsTo))
        )
        assertEquals(
            DetachDisplayInstanceFromJobUseCase.Result.JobNotStarted,
            DetachDisplayInstanceFromJobUseCase(manager).execute(displayInstance.id)
        )
        assertEquals(
            StopDisplayJobUseCase.Result.NotStarted,
            StopDisplayJobUseCase(RecordingDisplayScheduler(), manager).execute(displayInstance.belongsTo)
        )
    }

    private fun fixedClock(epochMillis: Long): Clock {
        return Clock.fixed(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC)
    }

    private class RecordingDisplayScheduler : DisplayScheduler {
        override val state: SchedulerState = SchedulerState.RUNNING
        var lastScheduledJob: DisplayJob? = null
        var lastAwakeAt: Instant? = null
        var lastUnscheduledJob: DisplayJob? = null

        override fun scheduleAwakeAt(job: DisplayJob, awakeAt: Instant) {
            lastScheduledJob = job
            lastAwakeAt = awakeAt
        }

        override fun unschedule(job: DisplayJob) {
            lastUnscheduledJob = job
        }

        override fun stop() = Unit
    }

    private class FakeDisplayJob(
        override val belongsTo: UUID,
    ) : DisplayJob {
        override var isStopped: Boolean = false
        override val managedDisplayInstances = linkedMapOf<UUID, DisplayInstance>()
        val attachedInstances = mutableListOf<DisplayInstance>()
        var stopCalled: Boolean = false

        override fun attach(displayInstance: DisplayInstance, image: Image, imageDataEntry: ImageDataEntry<*>) {
            require(image.id == belongsTo)
            require(imageDataEntry.imageId == belongsTo)
            managedDisplayInstances[displayInstance.id] = displayInstance
            attachedInstances += displayInstance
        }

        override fun detach(displayInstanceId: UUID): DisplayInstance? = managedDisplayInstances.remove(displayInstanceId)

        override fun isEmpty(): Boolean = managedDisplayInstances.isEmpty()

        override fun wake() = Unit

        override fun stop() {
            isStopped = true
            stopCalled = true
            managedDisplayInstances.clear()
        }
    }
}
