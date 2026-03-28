package plutoproject.feature.gallery.core

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.display.job.AnimatedDisplayJob
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.job.DisplayJob
import plutoproject.feature.gallery.core.display.DisplayManager
import plutoproject.feature.gallery.core.display.DisplayScheduler
import plutoproject.feature.gallery.core.display.ItemFrameFacing
import plutoproject.feature.gallery.core.display.MapUpdate
import plutoproject.feature.gallery.core.display.PlayerView
import plutoproject.feature.gallery.core.display.SchedulerState
import plutoproject.feature.gallery.core.display.Vec3
import plutoproject.feature.gallery.core.display.ViewPort
import plutoproject.feature.gallery.core.display.job.SendJob
import plutoproject.feature.gallery.core.display.job.SendJobState
import plutoproject.feature.gallery.core.image.AnimatedImageData
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.image.ImageType
import plutoproject.feature.gallery.core.image.TilePool
import plutoproject.feature.gallery.core.render.tile.codec.encodeTile
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

class AnimatedDisplayJobTest {
    @Test
    fun `wake should send first frame and schedule next awake with fps budget`() {
        val belongsTo = dummyUuid(6201)
        val image = animatedImage(belongsTo, intArrayOf(90))
        val entry = animatedImageDataEntry(
            belongsTo = belongsTo,
            frameTileColors = listOf(
                listOf(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 1 }),
                listOf(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 2 }),
            ),
            durationMillis = 200,
        )
        val scheduler = RecordingDisplayScheduler()
        val displayManager = DisplayManager().also {
            it.registerSendJob(RecordingSendJob(dummyUuid(6202)))
        }

        val job = AnimatedDisplayJob(
            belongsTo = belongsTo,
            displayScheduler = scheduler,
            viewPort = singleVisiblePlayerViewPort(dummyUuid(6202)),
            displayManager = displayManager,
            clock = MutableClock(0L),
            maxFramesPerSecond = 20,
            visibleDistance = 5.0,
            horizontalFovRadian = 0.0,
            verticalFovRadian = 0.0,
        )

        job.attach(singleTileDisplayInstance(belongsTo), image, entry)
        job.wake()

        val sendJob = displayManager.getLoadedSendJob(dummyUuid(6202)) as RecordingSendJob
        assertEquals(1, sendJob.enqueuedUpdates.size)
        assertEquals(90, sendJob.enqueuedUpdates.single().mapId)
        assertArrayEquals(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 1 }, sendJob.enqueuedUpdates.single().mapColors)
        assertEquals(Instant.ofEpochMilli(50L), scheduler.lastAwakeAt)
    }

    @Test
    fun `wake should only send changed tiles for visible players`() {
        val belongsTo = dummyUuid(6211)
        val image = Image(
            id = belongsTo,
            type = ImageType.ANIMATED,
            owner = dummyUuid(6210),
            ownerName = "Owner_6210",
            name = "animated-job-test",
            mapWidthBlocks = 2,
            mapHeightBlocks = 1,
            tileMapIds = intArrayOf(101, 102),
        )
        val frameOneTileA = ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 5 }
        val frameOneTileB = ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 6 }
        val frameTwoTileA = ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 5 }
        val frameTwoTileB = ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 7 }
        val entry = animatedImageDataEntry(
            belongsTo = belongsTo,
            frameTileColors = listOf(
                listOf(frameOneTileA, frameOneTileB),
                listOf(frameTwoTileA, frameTwoTileB),
            ),
            durationMillis = 200,
        )
        val clock = MutableClock(0L)
        val scheduler = RecordingDisplayScheduler()
        val displayManager = DisplayManager().also {
            it.registerSendJob(RecordingSendJob(dummyUuid(6212)))
        }

        val job = AnimatedDisplayJob(
            belongsTo = belongsTo,
            displayScheduler = scheduler,
            viewPort = singleVisiblePlayerViewPort(dummyUuid(6212), eye = Vec3(0.5, 0.0, 1.0)),
            displayManager = displayManager,
            clock = clock,
            maxFramesPerSecond = 20,
            visibleDistance = 5.0,
            horizontalFovRadian = 0.0,
            verticalFovRadian = 0.0,
        )

        job.attach(
            DisplayInstance(
                id = dummyUuid(6213),
                belongsTo = belongsTo,
                world = "world",
                chunkX = 0,
                chunkZ = 0,
                facing = ItemFrameFacing.SOUTH,
                widthBlocks = 2,
                heightBlocks = 1,
                originX = 0.0,
                originY = 0.0,
                originZ = 0.0,
                itemFrameIds = listOf(dummyUuid(6214), dummyUuid(6215)),
            ),
            image,
            entry,
        )

        job.wake()
        val sendJob = displayManager.getLoadedSendJob(dummyUuid(6212)) as RecordingSendJob
        assertEquals(listOf(101, 102), sendJob.enqueuedUpdates.map(MapUpdate::mapId))

        clock.currentMillis = 100L
        job.wake()

        assertEquals(listOf(101, 102, 102), sendJob.enqueuedUpdates.map(MapUpdate::mapId))
        assertArrayEquals(frameTwoTileB, sendJob.enqueuedUpdates.last().mapColors)
    }

    @Test
    fun `wake should schedule immediately when fps is unlimited and attach should reject after stop`() {
        val belongsTo = dummyUuid(6221)
        val image = animatedImage(belongsTo, intArrayOf(110))
        val entry = animatedImageDataEntry(
            belongsTo = belongsTo,
            frameTileColors = listOf(listOf(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 8 })),
            durationMillis = 100,
        )
        val clock = MutableClock(200L)
        val scheduler = RecordingDisplayScheduler()
        val displayManager = DisplayManager().also {
            it.registerSendJob(RecordingSendJob(dummyUuid(6222)))
        }

        val job = AnimatedDisplayJob(
            belongsTo = belongsTo,
            displayScheduler = scheduler,
            viewPort = singleVisiblePlayerViewPort(dummyUuid(6222)),
            displayManager = displayManager,
            clock = clock,
            maxFramesPerSecond = -1,
            visibleDistance = 5.0,
            horizontalFovRadian = 0.0,
            verticalFovRadian = 0.0,
        )
        val displayInstance = singleTileDisplayInstance(belongsTo)

        job.attach(displayInstance, image, entry)
        job.wake()
        assertEquals(Instant.ofEpochMilli(200L), scheduler.lastAwakeAt)

        job.stop()
        assertThrows(IllegalStateException::class.java) {
            job.attach(displayInstance, image, entry)
        }
    }

    @Test
    fun `wake should keep fixed started time and loop progress with modulo`() {
        val belongsTo = dummyUuid(6231)
        val image = animatedImage(belongsTo, intArrayOf(120))
        val entry = animatedImageDataEntry(
            belongsTo = belongsTo,
            frameTileColors = listOf(
                listOf(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 1 }),
                listOf(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 2 }),
            ),
            durationMillis = 200,
        )
        val clock = MutableClock(0L)
        val displayManager = DisplayManager().also {
            it.registerSendJob(RecordingSendJob(dummyUuid(6232)))
        }
        val job = AnimatedDisplayJob(
            belongsTo = belongsTo,
            displayScheduler = RecordingDisplayScheduler(),
            viewPort = singleVisiblePlayerViewPort(dummyUuid(6232)),
            displayManager = displayManager,
            clock = clock,
            maxFramesPerSecond = 20,
            visibleDistance = 5.0,
            horizontalFovRadian = 0.0,
            verticalFovRadian = 0.0,
        )

        job.attach(singleTileDisplayInstance(belongsTo), image, entry)
        job.wake()

        clock.currentMillis = 350L
        job.wake()

        val sendJob = displayManager.getLoadedSendJob(dummyUuid(6232)) as RecordingSendJob
        assertEquals(listOf(120, 120), sendJob.enqueuedUpdates.map(MapUpdate::mapId))
        assertArrayEquals(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 2 }, sendJob.enqueuedUpdates.last().mapColors)
    }

    private fun animatedImage(belongsTo: UUID, tileMapIds: IntArray): Image {
        return Image(
            id = belongsTo,
            type = ImageType.ANIMATED,
            owner = dummyUuid(6200),
            ownerName = "Owner_6200",
            name = "animated-image",
            mapWidthBlocks = tileMapIds.size,
            mapHeightBlocks = 1,
            tileMapIds = tileMapIds,
        )
    }

    private fun animatedImageDataEntry(
        belongsTo: UUID,
        frameTileColors: List<List<ByteArray>>,
        durationMillis: Int,
    ): ImageDataEntry<*> {
        val encodedTiles = linkedMapOf<String, ByteArray>()
        val frameIndexes = mutableListOf<Short>()

        frameTileColors.flatten().forEach { tileColors ->
            val key = tileColors.contentToString()
            val tileIndex = encodedTiles.keys.indexOf(key)
            if (tileIndex >= 0) {
                frameIndexes += tileIndex.toShort()
            } else {
                encodedTiles[key] = encodeTile(tileColors)
                frameIndexes += (encodedTiles.size - 1).toShort()
            }
        }

        val offsets = IntArray(encodedTiles.size + 1)
        var totalBytes = 0
        encodedTiles.values.forEachIndexed { index, tileData ->
            offsets[index] = totalBytes
            totalBytes += tileData.size
        }
        offsets[encodedTiles.size] = totalBytes

        val blob = ByteArray(totalBytes)
        var cursor = 0
        encodedTiles.values.forEach { tileData ->
            tileData.copyInto(blob, cursor)
            cursor += tileData.size
        }

        return ImageDataEntry(
            belongsTo = belongsTo,
            type = ImageType.ANIMATED,
            data = AnimatedImageData(
                frameCount = frameTileColors.size,
                durationMillis = durationMillis,
                tilePool = TilePool(offsets = offsets, blob = blob),
                tileIndexes = frameIndexes.toShortArray(),
            ),
        )
    }

    private fun singleVisiblePlayerViewPort(playerId: UUID, eye: Vec3 = Vec3(0.0, 0.0, 1.0)): ViewPort {
        return object : ViewPort {
            override fun getPlayerViews(world: String): List<PlayerView> {
                return listOf(PlayerView(playerId, eye, Vec3(0.0, 0.0, -1.0)))
            }
        }
    }

    private fun singleTileDisplayInstance(belongsTo: UUID): DisplayInstance {
        return DisplayInstance(
            id = dummyUuid(6223),
            belongsTo = belongsTo,
            world = "world",
            chunkX = 0,
            chunkZ = 0,
            facing = ItemFrameFacing.SOUTH,
            widthBlocks = 1,
            heightBlocks = 1,
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            itemFrameIds = listOf(dummyUuid(6224)),
        )
    }

    private class MutableClock(
        var currentMillis: Long,
    ) : Clock() {
        override fun getZone() = ZoneOffset.UTC

        override fun withZone(zone: ZoneId?): Clock = this

        override fun instant(): Instant = Instant.ofEpochMilli(currentMillis)
    }

    private class RecordingDisplayScheduler : DisplayScheduler {
        override val state: SchedulerState = SchedulerState.RUNNING
        var lastScheduledJob: DisplayJob? = null
        var lastAwakeAt: Instant? = null

        override fun scheduleAwakeAt(job: DisplayJob, awakeAt: Instant) {
            lastScheduledJob = job
            lastAwakeAt = awakeAt
        }

        override fun unschedule(job: DisplayJob) = Unit

        override fun stop() = Unit
    }

    private class RecordingSendJob(
        override val playerId: UUID,
    ) : SendJob {
        override val state: SendJobState = SendJobState.IDLING
        val enqueuedUpdates = mutableListOf<MapUpdate>()

        override fun enqueue(update: MapUpdate) {
            enqueuedUpdates += update
        }

        override fun stop() = Unit
    }
}
