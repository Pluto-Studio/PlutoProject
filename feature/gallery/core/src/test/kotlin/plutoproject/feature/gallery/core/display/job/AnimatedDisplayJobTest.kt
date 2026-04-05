package plutoproject.feature.gallery.core.display.job

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import plutoproject.feature.gallery.core.image.ImageType
import plutoproject.feature.gallery.core.newDisplayRuntime
import plutoproject.feature.gallery.core.render.tile.TilePool
import plutoproject.feature.gallery.core.render.tile.TilePoolSnapshot
import plutoproject.feature.gallery.core.render.tile.codec.TileEncoder
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class AnimatedDisplayJobTest {
    @Test
    fun `wake should send first frame and schedule next awake`() = runTest {
        val sentUpdates = mutableListOf<MapUpdate>()
        val playerId = dummyUuid(6202)
        val clock = MutableClock(0L)
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
            clock = clock,
            mapUpdatePort = recordingMapUpdatePort(sentUpdates),
            viewPort = singleVisiblePlayerViewPort(playerId),
        )
        try {
            runtime.sendJobRegistry.start(playerId)
            val imageId = dummyUuid(6201)
            val job = AnimatedDisplayJob(
                imageId = imageId,
                image = animatedImage(imageId, intArrayOf(90)),
                initialResource = animatedResource(
                    frameTileColors = listOf(
                        listOf(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 1 }),
                        listOf(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 2 }),
                    ),
                    durationMillis = 200,
                ),
                displayScheduler = runtime.scheduler,
                viewPort = singleVisiblePlayerViewPort(playerId),
                sendJobRegistry = runtime.sendJobRegistry,
                clock = clock,
                maxFramesPerSecond = 20,
                visibleDistance = 5.0,
            )

            job.attach(singleTileDisplayInstance(imageId))
            schedulerDispatcher.scheduler.runCurrent()
            schedulerDispatcher.scheduler.runCurrent()

            assertEquals(1, sentUpdates.size)
            assertEquals(90, sentUpdates.single().mapId)
            assertArrayEquals(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 1 }, sentUpdates.single().mapColors)
            assertEquals(SchedulerState.RUNNING, runtime.scheduler.state)
        } finally {
            runtime.runtimeRegistry.close()
            runtime.sendJobRegistry.close()
            runtime.scheduler.stop()
            scope.cancel()
            advanceUntilIdle()
        }
    }

    @Test
    fun `wake should only send changed tiles for visible players`() = runTest {
        val sentUpdates = mutableListOf<MapUpdate>()
        val playerId = dummyUuid(6212)
        val clock = MutableClock(0L)
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
            clock = clock,
            mapUpdatePort = recordingMapUpdatePort(sentUpdates),
            viewPort = object : ViewPort {
                override fun getPlayerViews(world: String): List<PlayerView> {
                    return listOf(PlayerView(playerId, Vec3(0.5, 0.0, 1.0), Vec3(0.0, 0.0, -1.0)))
                }
            },
        )
        try {
            runtime.sendJobRegistry.start(playerId)
            val imageId = dummyUuid(6211)
            val image = Image(
                id = imageId,
                type = ImageType.ANIMATED,
                owner = dummyUuid(6210),
                ownerName = "Owner_6210",
                name = "animated-job-test",
                widthBlocks = 2,
                heightBlocks = 1,
                tileMapIds = intArrayOf(101, 102),
            )
            val frameOneTileA = ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 5 }
            val frameOneTileB = ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 6 }
            val frameTwoTileA = ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 5 }
            val frameTwoTileB = ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 7 }
            val job = AnimatedDisplayJob(
                imageId = imageId,
                image = image,
                initialResource = animatedResource(
                    frameTileColors = listOf(
                        listOf(frameOneTileA, frameOneTileB),
                        listOf(frameTwoTileA, frameTwoTileB),
                    ),
                    durationMillis = 200,
                ),
                displayScheduler = runtime.scheduler,
                viewPort = object : ViewPort {
                    override fun getPlayerViews(world: String): List<PlayerView> {
                        return listOf(PlayerView(playerId, Vec3(0.5, 0.0, 1.0), Vec3(0.0, 0.0, -1.0)))
                    }
                },
                sendJobRegistry = runtime.sendJobRegistry,
                clock = clock,
                maxFramesPerSecond = 20,
                visibleDistance = 5.0,
            )

            job.attach(
                DisplayInstance(
                    id = dummyUuid(6213),
                    imageId = imageId,
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
                )
            )

            schedulerDispatcher.scheduler.runCurrent()
            schedulerDispatcher.scheduler.runCurrent()
            assertEquals(listOf(101, 102), sentUpdates.map(MapUpdate::mapId))

            clock.currentMillis = 100L
            job.wake()
            schedulerDispatcher.scheduler.runCurrent()

            assertEquals(listOf(101, 102, 102), sentUpdates.map(MapUpdate::mapId))
            assertArrayEquals(frameTwoTileB, sentUpdates.last().mapColors)
        } finally {
            runtime.runtimeRegistry.close()
            runtime.sendJobRegistry.close()
            runtime.scheduler.stop()
            scope.cancel()
            advanceUntilIdle()
        }
    }

    @Test
    fun `replace resource should reset animation progress`() = runTest {
        val sentUpdates = mutableListOf<MapUpdate>()
        val playerId = dummyUuid(6218)
        val clock = MutableClock(0L)
        val runtime = newDisplayRuntime(
            this,
            clock = clock,
            mapUpdatePort = recordingMapUpdatePort(sentUpdates),
            viewPort = singleVisiblePlayerViewPort(playerId),
        )
        val imageId = dummyUuid(6217)
        val job = AnimatedDisplayJob(
            imageId = imageId,
            image = animatedImage(imageId, intArrayOf(111)),
            initialResource = animatedResource(
                frameTileColors = listOf(
                    listOf(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 1 }),
                    listOf(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 2 }),
                ),
                durationMillis = 200,
            ),
            displayScheduler = runtime.scheduler,
            viewPort = singleVisiblePlayerViewPort(playerId),
            sendJobRegistry = runtime.sendJobRegistry,
            clock = clock,
            maxFramesPerSecond = 20,
            visibleDistance = 5.0,
        )

        try {
            runtime.sendJobRegistry.start(playerId)
            job.attach(singleTileDisplayInstance(imageId))
            runCurrent()
            runCurrent()
            clock.currentMillis = 100L
            job.wake()
            job.replaceResource(
                animatedResource(
                    frameTileColors = listOf(
                        listOf(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 9 }),
                        listOf(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 10 }),
                    ),
                    durationMillis = 200,
                )
            )
            clock.currentMillis = 100L
            runCurrent()
            runCurrent()

            assertArrayEquals(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 9 }, sentUpdates.last().mapColors)
        } finally {
            runtime.runtimeRegistry.close()
            runtime.sendJobRegistry.close()
            runtime.scheduler.stop()
        }
    }

    @Test
    fun `wake should schedule immediately when fps is unlimited and attach should reject after stop`() = runTest {
        val sentUpdates = mutableListOf<MapUpdate>()
        val playerId = dummyUuid(6222)
        val clock = MutableClock(200L)
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
            clock = clock,
            mapUpdatePort = recordingMapUpdatePort(sentUpdates),
            viewPort = singleVisiblePlayerViewPort(playerId),
        )
        try {
            runtime.sendJobRegistry.start(playerId)
            val imageId = dummyUuid(6221)
            val job = AnimatedDisplayJob(
                imageId = imageId,
                image = animatedImage(imageId, intArrayOf(110)),
                initialResource = animatedResource(
                    frameTileColors = listOf(listOf(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 8 })),
                    durationMillis = 100,
                ),
                displayScheduler = runtime.scheduler,
                viewPort = singleVisiblePlayerViewPort(playerId),
                sendJobRegistry = runtime.sendJobRegistry,
                clock = clock,
                maxFramesPerSecond = -1,
                visibleDistance = 5.0,
            )
            val displayInstance = singleTileDisplayInstance(imageId)

            job.attach(displayInstance)
            assertEquals(SchedulerState.RUNNING, runtime.scheduler.state)

            job.stop()
            assertThrows(IllegalStateException::class.java) {
                job.attach(displayInstance)
            }
        } finally {
            runtime.runtimeRegistry.close()
            runtime.sendJobRegistry.close()
            runtime.scheduler.stop()
            scope.cancel()
            advanceUntilIdle()
        }
    }

    @Test
    fun `wake should keep fixed started time and loop progress with modulo`() = runTest {
        val sentUpdates = mutableListOf<MapUpdate>()
        val playerId = dummyUuid(6232)
        val clock = MutableClock(0L)
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
            clock = clock,
            mapUpdatePort = recordingMapUpdatePort(sentUpdates),
            viewPort = singleVisiblePlayerViewPort(playerId),
        )
        try {
            runtime.sendJobRegistry.start(playerId)
            val imageId = dummyUuid(6231)
            val job = AnimatedDisplayJob(
                imageId = imageId,
                image = animatedImage(imageId, intArrayOf(120)),
                initialResource = animatedResource(
                    frameTileColors = listOf(
                        listOf(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 1 }),
                        listOf(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 2 }),
                    ),
                    durationMillis = 200,
                ),
                displayScheduler = runtime.scheduler,
                viewPort = singleVisiblePlayerViewPort(playerId),
                sendJobRegistry = runtime.sendJobRegistry,
                clock = clock,
                maxFramesPerSecond = 20,
                visibleDistance = 5.0,
            )

            job.attach(singleTileDisplayInstance(imageId))
            schedulerDispatcher.scheduler.runCurrent()
            schedulerDispatcher.scheduler.runCurrent()

            clock.currentMillis = 350L
            job.wake()
            advanceTimeBy(50)
            advanceUntilIdle()

            assertEquals(listOf(120, 120), sentUpdates.map(MapUpdate::mapId))
            assertArrayEquals(ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 2 }, sentUpdates.last().mapColors)
        } finally {
            runtime.runtimeRegistry.close()
            runtime.sendJobRegistry.close()
            runtime.scheduler.stop()
            scope.cancel()
            advanceUntilIdle()
        }
    }

    @Test
    fun `animated resource should reject non-positive frame metadata`() {
        assertThrows(IllegalArgumentException::class.java) {
            DisplayResourceFactory().create(
                ImageData.Animated(
                    tilePool = TilePool.fromSnapshot(TilePoolSnapshot(offsets = intArrayOf(0), blob = ByteArray(0))),
                    tileIndexes = shortArrayOf(),
                    frameCount = 0,
                    duration = 100.milliseconds,
                )
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            DisplayResourceFactory().create(
                ImageData.Animated(
                    tilePool = TilePool.fromSnapshot(TilePoolSnapshot(offsets = intArrayOf(0), blob = ByteArray(0))),
                    tileIndexes = shortArrayOf(),
                    frameCount = 1,
                    duration = 0.milliseconds,
                )
            )
        }
    }

    private fun animatedImage(imageId: UUID, tileMapIds: IntArray): Image {
        return Image(
            id = imageId,
            type = ImageType.ANIMATED,
            owner = dummyUuid(6200),
            ownerName = "Owner_6200",
            name = "animated-image",
            widthBlocks = tileMapIds.size,
            heightBlocks = 1,
            tileMapIds = tileMapIds,
        )
    }

    private fun animatedResource(
        frameTileColors: List<List<ByteArray>>,
        durationMillis: Int,
    ): AnimatedDisplayResource {
        val encodedTiles = linkedMapOf<String, ByteArray>()
        val frameIndexes = mutableListOf<Short>()

        frameTileColors.flatten().forEach { tileColors ->
            val key = tileColors.contentToString()
            val tileIndex = encodedTiles.keys.indexOf(key)
            if (tileIndex >= 0) {
                frameIndexes += tileIndex.toShort()
            } else {
                encodedTiles[key] = TileEncoder.encode(tileColors)
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

        return DisplayResourceFactory().create(
            ImageData.Animated(
                tilePool = TilePool.fromSnapshot(TilePoolSnapshot(offsets = offsets, blob = blob)),
                tileIndexes = frameIndexes.toShortArray(),
                frameCount = frameTileColors.size,
                duration = durationMillis.milliseconds,
            )
        ) as AnimatedDisplayResource
    }

    private fun singleVisiblePlayerViewPort(playerId: UUID, eye: Vec3 = Vec3(0.0, 0.0, 1.0)): ViewPort {
        return object : ViewPort {
            override fun getPlayerViews(world: String): List<PlayerView> {
                return listOf(PlayerView(playerId, eye, Vec3(0.0, 0.0, -1.0)))
            }
        }
    }

    private fun singleTileDisplayInstance(imageId: UUID): DisplayInstance {
        return DisplayInstance(
            id = dummyUuid(6223),
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
            itemFrameIds = listOf(dummyUuid(6224)),
        )
    }

    private fun recordingMapUpdatePort(sentUpdates: MutableList<MapUpdate>): MapUpdatePort {
        return object : MapUpdatePort {
            override fun send(playerId: UUID, update: MapUpdate) {
                sentUpdates += update
            }
        }
    }

    private class MutableClock(
        var currentMillis: Long,
    ) : Clock() {
        override fun getZone() = ZoneOffset.UTC

        override fun withZone(zone: ZoneId?): Clock = this

        override fun instant(): Instant = Instant.ofEpochMilli(currentMillis)
    }
}
