package plutoproject.feature.gallery.core

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.render.tile.encodeTile
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class StaticDisplayJobTest {
    @Test
    fun `wake should send visible static tiles and schedule next awake`() {
        val belongsTo = dummyUuid(6101)
        val image = Image(
            id = belongsTo,
            type = ImageType.STATIC,
            owner = dummyUuid(6100),
            ownerName = "Owner_6100",
            name = "static-job-test-1",
            mapWidthBlocks = 1,
            mapHeightBlocks = 1,
            tileMapIds = intArrayOf(77),
        )
        val entry = staticImageDataEntry(
            belongsTo = belongsTo,
            tileColors = listOf(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 7 }),
        )
        val displayInstance = DisplayInstance(
            id = dummyUuid(6102),
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
            itemFrameIds = listOf(dummyUuid(6104)),
        )
        val scheduler = RecordingDisplayScheduler()
        val displayManager = DisplayManager()
        val sendJob = RecordingSendJob(playerId = dummyUuid(6103))
        displayManager.registerSendJob(sendJob)

        val job = StaticDisplayJob(
            belongsTo = belongsTo,
            displayScheduler = scheduler,
            viewPort = FakeViewPort(
                playerViewsByWorld = mapOf(
                    "world" to listOf(
                        PlayerView(
                            id = sendJob.playerId,
                            eye = Vec3(0.0, 0.0, 1.0),
                            viewDirection = Vec3(0.0, 0.0, -1.0),
                        )
                    )
                )
            ),
            displayManager = displayManager,
            clock = fixedClock(1_000L),
            visibleDistance = 5.0,
            horizontalFovRadian = 0.0,
            verticalFovRadian = 0.0,
            updateIntervalMs = 200L,
        )

        job.attach(displayInstance, image, entry)
        job.wake()

        assertEquals(1, sendJob.enqueuedUpdates.size)
        assertEquals(77, sendJob.enqueuedUpdates.single().mapId)
        assertArrayEquals(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 7 }, sendJob.enqueuedUpdates.single().mapColors)
        assertEquals(job, scheduler.lastScheduledJob)
        assertEquals(Instant.ofEpochMilli(1_200L), scheduler.lastAwakeAt)
    }

    @Test
    fun `wake should not resend static map ids already received by player`() {
        val belongsTo = dummyUuid(6111)
        val image = Image(
            id = belongsTo,
            type = ImageType.STATIC,
            owner = dummyUuid(6110),
            ownerName = "Owner_6110",
            name = "static-job-test-2",
            mapWidthBlocks = 1,
            mapHeightBlocks = 1,
            tileMapIds = intArrayOf(88),
        )
        val entry = staticImageDataEntry(
            belongsTo = belongsTo,
            tileColors = listOf(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 9 }),
        )
        val displayInstance = DisplayInstance(
            id = dummyUuid(6112),
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
            itemFrameIds = listOf(dummyUuid(6114)),
        )
        val displayManager = DisplayManager()
        val sendJob = RecordingSendJob(playerId = dummyUuid(6113))
        displayManager.registerSendJob(sendJob)

        val job = StaticDisplayJob(
            belongsTo = belongsTo,
            displayScheduler = RecordingDisplayScheduler(),
            viewPort = FakeViewPort(
                mapOf(
                    "world" to listOf(
                        PlayerView(sendJob.playerId, Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, -1.0))
                    )
                )
            ),
            displayManager = displayManager,
            clock = fixedClock(2_000L),
            visibleDistance = 5.0,
            horizontalFovRadian = 0.0,
            verticalFovRadian = 0.0,
            updateIntervalMs = 200L,
        )

        job.attach(displayInstance, image, entry)
        job.wake()
        job.wake()

        assertEquals(1, sendJob.enqueuedUpdates.size)
    }

    @Test
    fun `wake should skip invisible players and attach should reject after stop`() {
        val belongsTo = dummyUuid(6121)
        val image = Image(
            id = belongsTo,
            type = ImageType.STATIC,
            owner = dummyUuid(6120),
            ownerName = "Owner_6120",
            name = "static-job-test-3",
            mapWidthBlocks = 1,
            mapHeightBlocks = 1,
            tileMapIds = intArrayOf(99),
        )
        val entry = staticImageDataEntry(
            belongsTo = belongsTo,
            tileColors = listOf(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 11 }),
        )
        val displayInstance = DisplayInstance(
            id = dummyUuid(6122),
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
            itemFrameIds = listOf(dummyUuid(6124)),
        )
        val displayManager = DisplayManager()
        val sendJob = RecordingSendJob(playerId = dummyUuid(6123))
        displayManager.registerSendJob(sendJob)

        val job = StaticDisplayJob(
            belongsTo = belongsTo,
            displayScheduler = RecordingDisplayScheduler(),
            viewPort = FakeViewPort(
                mapOf(
                    "world" to listOf(
                        PlayerView(sendJob.playerId, Vec3(0.0, 0.0, -1.0), Vec3(0.0, 0.0, -1.0))
                    )
                )
            ),
            displayManager = displayManager,
            clock = fixedClock(3_000L),
            visibleDistance = 5.0,
            horizontalFovRadian = 0.0,
            verticalFovRadian = 0.0,
            updateIntervalMs = 200L,
        )

        job.attach(displayInstance, image, entry)
        job.wake()

        assertTrue(sendJob.enqueuedUpdates.isEmpty())

        job.stop()
        assertThrows(IllegalStateException::class.java) {
            job.attach(displayInstance, image, entry)
        }
    }

    @Test
    fun `wake should merge visible tiles from multiple instances of same image`() {
        val belongsTo = dummyUuid(6131)
        val image = Image(
            id = belongsTo,
            type = ImageType.STATIC,
            owner = dummyUuid(6130),
            ownerName = "Owner_6130",
            name = "static-job-test-4",
            mapWidthBlocks = 2,
            mapHeightBlocks = 1,
            tileMapIds = intArrayOf(131, 132),
        )
        val entry = staticImageDataEntry(
            belongsTo = belongsTo,
            tileColors = listOf(
                ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 21 },
                ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 22 },
            ),
        )
        val firstDisplayInstance = DisplayInstance(
            id = dummyUuid(6132),
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
            itemFrameIds = listOf(dummyUuid(6134), dummyUuid(6135)),
        )
        val secondDisplayInstance = DisplayInstance(
            id = dummyUuid(6133),
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
            itemFrameIds = listOf(dummyUuid(6136), dummyUuid(6137)),
        )
        val displayManager = DisplayManager()
        val sendJob = RecordingSendJob(playerId = dummyUuid(6138))
        displayManager.registerSendJob(sendJob)

        val job = StaticDisplayJob(
            belongsTo = belongsTo,
            displayScheduler = RecordingDisplayScheduler(),
            viewPort = FakeViewPort(
                mapOf(
                    "world" to listOf(
                        PlayerView(sendJob.playerId, Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, -1.0)),
                        PlayerView(sendJob.playerId, Vec3(0.5, 0.0, 1.0), Vec3(0.0, 0.0, -1.0)),
                    )
                )
            ),
            displayManager = displayManager,
            clock = fixedClock(4_000L),
            visibleDistance = 5.0,
            horizontalFovRadian = 0.0,
            verticalFovRadian = 0.0,
            updateIntervalMs = 200L,
        )

        job.attach(firstDisplayInstance, image, entry)
        job.attach(secondDisplayInstance, image, entry)
        job.wake()

        assertEquals(listOf(131, 132), sendJob.enqueuedUpdates.map(MapUpdate::mapId))
        assertArrayEquals(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 21 }, sendJob.enqueuedUpdates[0].mapColors)
        assertArrayEquals(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 22 }, sendJob.enqueuedUpdates[1].mapColors)
    }

    private fun fixedClock(epochMillis: Long): Clock {
        return Clock.fixed(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC)
    }

    private fun staticImageDataEntry(
        belongsTo: UUID,
        tileColors: List<ByteArray>,
    ): ImageDataEntry<*> {
        val encodedTiles = tileColors.map(::encodeTile)
        val offsets = IntArray(encodedTiles.size + 1)
        var totalBytes = 0
        encodedTiles.forEachIndexed { index, tileData ->
            offsets[index] = totalBytes
            totalBytes += tileData.size
        }
        offsets[encodedTiles.size] = totalBytes

        val blob = ByteArray(totalBytes)
        var cursor = 0
        encodedTiles.forEach { tileData ->
            tileData.copyInto(blob, cursor)
            cursor += tileData.size
        }

        return ImageDataEntry(
            belongsTo = belongsTo,
            type = ImageType.STATIC,
            data = StaticImageData(
                tilePool = TilePool(offsets = offsets, blob = blob),
                tileIndexes = ShortArray(tileColors.size) { it.toShort() },
            ),
        )
    }

    private class FakeViewPort(
        private val playerViewsByWorld: Map<String, List<PlayerView>>,
    ) : ViewPort {
        override fun getPlayerViews(world: String): List<PlayerView> {
            return playerViewsByWorld[world].orEmpty()
        }
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
