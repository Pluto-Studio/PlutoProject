package plutoproject.feature.gallery.core.display.job

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.ItemFrameFacing
import plutoproject.feature.gallery.core.display.MapUpdate
import plutoproject.feature.gallery.core.display.MapUpdatePort
import plutoproject.feature.gallery.core.display.PlayerView
import plutoproject.feature.gallery.core.display.SchedulerState
import plutoproject.feature.gallery.core.display.Vec3
import plutoproject.feature.gallery.core.display.ViewPort
import plutoproject.feature.gallery.core.dummyUuid
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageData
import plutoproject.feature.gallery.core.image.ImageDataEntry
import plutoproject.feature.gallery.core.image.ImageType
import plutoproject.feature.gallery.core.newDisplayRuntime
import plutoproject.feature.gallery.core.render.tile.TilePool
import plutoproject.feature.gallery.core.render.tile.TilePoolSnapshot
import plutoproject.feature.gallery.core.render.tile.codec.TileEncoder
import plutoproject.feature.gallery.core.schedulerClock
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class StaticDisplayJobTest {
    @Test
    fun `wake should send visible static tiles and schedule next awake`() = runTest {
        val sentUpdates = mutableListOf<MapUpdate>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val schedulerDispatcher = StandardTestDispatcher(TestCoroutineScheduler())
        val runtime = newDisplayRuntime(
            scope = scope,
            coroutineContext = Dispatchers.Unconfined,
            schedulerScope = scope,
            schedulerContext = schedulerDispatcher,
            awakeContext = schedulerDispatcher,
            sendScope = scope,
            sendLoopContext = Dispatchers.Unconfined,
            clock = schedulerClock(this),
            mapUpdatePort = recordingMapUpdatePort(sentUpdates),
            viewPort = singleVisiblePlayerViewPort(dummyUuid(6103)),
        )
        val belongsTo = dummyUuid(6101)
        val image = staticImage(belongsTo, intArrayOf(77))
        val entry = staticImageDataEntry(belongsTo, listOf(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 7 }))
        val displayInstance = singleTileDisplayInstance(belongsTo, dummyUuid(6102), dummyUuid(6104))
        try {
            runtime.manager.startSendJob(dummyUuid(6103))

            val job = StaticDisplayJob(
                imageId = belongsTo,
                image = image,
                imageDataEntry = entry.asStatic(),
                displayScheduler = runtime.scheduler,
                viewPort = singleVisiblePlayerViewPort(dummyUuid(6103)),
                displayManager = runtime.manager,
                clock = schedulerClock(this),
                visibleDistance = 5.0,
                updateInterval = 200.milliseconds,
            )

            job.attach(displayInstance)
            job.wake()
            runCurrent()

            assertEquals(1, sentUpdates.size)
            assertEquals(77, sentUpdates.single().mapId)
            assertArrayEquals(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 7 }, sentUpdates.single().mapColors)
            assertEquals(SchedulerState.RUNNING, runtime.scheduler.state)

            testScheduler.advanceTimeBy(200)
            advanceUntilIdle()
            assertEquals(1, sentUpdates.size)
        } finally {
            runtime.manager.close()
            runtime.scheduler.stop()
            scope.cancel()
            advanceUntilIdle()
        }
    }

    @Test
    fun `wake should skip invisible players and attach should reject after stop`() = runTest {
        val sentUpdates = mutableListOf<MapUpdate>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val schedulerDispatcher = StandardTestDispatcher(TestCoroutineScheduler())
        val runtime = newDisplayRuntime(
            scope = scope,
            coroutineContext = Dispatchers.Unconfined,
            schedulerScope = scope,
            schedulerContext = schedulerDispatcher,
            awakeContext = schedulerDispatcher,
            sendScope = scope,
            sendLoopContext = Dispatchers.Unconfined,
            clock = schedulerClock(this),
            mapUpdatePort = recordingMapUpdatePort(sentUpdates),
            viewPort = object : ViewPort {
                override fun getPlayerViews(world: String): List<PlayerView> {
                    return listOf(PlayerView(dummyUuid(6123), Vec3(0.0, 0.0, -1.0), Vec3(0.0, 0.0, -1.0)))
                }
            },
        )
        val belongsTo = dummyUuid(6121)
        try {
            runtime.manager.startSendJob(dummyUuid(6123))
            val job = StaticDisplayJob(
                imageId = belongsTo,
                image = staticImage(belongsTo, intArrayOf(99)),
                imageDataEntry = staticImageDataEntry(belongsTo, listOf(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 11 })).asStatic(),
                displayScheduler = runtime.scheduler,
                viewPort = object : ViewPort {
                    override fun getPlayerViews(world: String): List<PlayerView> {
                        return listOf(PlayerView(dummyUuid(6123), Vec3(0.0, 0.0, -1.0), Vec3(0.0, 0.0, -1.0)))
                    }
                },
                displayManager = runtime.manager,
                clock = schedulerClock(this),
                visibleDistance = 5.0,
                updateInterval = 200.milliseconds,
            )
            val displayInstance = singleTileDisplayInstance(belongsTo, dummyUuid(6122), dummyUuid(6124))

            job.attach(displayInstance)
            job.wake()
            advanceUntilIdle()
            assertTrue(sentUpdates.isEmpty())

            job.stop()
            assertThrows(IllegalStateException::class.java) {
                job.attach(displayInstance)
            }
        } finally {
            runtime.manager.close()
            runtime.scheduler.stop()
            scope.cancel()
            advanceUntilIdle()
        }
    }

    private fun staticImage(imageId: UUID, tileMapIds: IntArray): Image {
        return Image(
            id = imageId,
            type = ImageType.STATIC,
            owner = dummyUuid(6100),
            ownerName = "Owner_6100",
            name = "static-job-test",
            widthBlocks = tileMapIds.size,
            heightBlocks = 1,
            tileMapIds = tileMapIds,
        )
    }

    private fun staticImageDataEntry(imageId: UUID, tileColors: List<ByteArray>): ImageDataEntry.Static {
        val encodedTiles = tileColors.map(TileEncoder::encode)
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

        return ImageDataEntry.Static(
            imageId = imageId,
            data = ImageData.Static(
                tilePool = TilePool.fromSnapshot(TilePoolSnapshot(offsets = offsets, blob = blob)),
                tileIndexes = ShortArray(tileColors.size) { it.toShort() },
            ),
        )
    }

    private fun singleVisiblePlayerViewPort(playerId: UUID): ViewPort {
        return object : ViewPort {
            override fun getPlayerViews(world: String): List<PlayerView> {
                return listOf(PlayerView(playerId, Vec3(0.0, 0.0, 1.0), Vec3(0.0, 0.0, -1.0)))
            }
        }
    }

    private fun singleTileDisplayInstance(imageId: UUID, id: UUID, frameId: UUID): DisplayInstance {
        return DisplayInstance(
            id = id,
            imageId = imageId,
            world = "world",
            chunkX = 0,
            chunkZ = 0,
            facing = ItemFrameFacing.SOUTH,
            widthBlocks = 1,
            heightBlocks = 1,
            originX = 0.0,
            originY = 0.0,
            originZ = 0.0,
            itemFrameIds = listOf(frameId),
        )
    }

    private fun recordingMapUpdatePort(sentUpdates: MutableList<MapUpdate>): MapUpdatePort {
        return object : MapUpdatePort {
            override fun send(playerId: UUID, update: MapUpdate) {
                sentUpdates += update
            }
        }
    }
}
