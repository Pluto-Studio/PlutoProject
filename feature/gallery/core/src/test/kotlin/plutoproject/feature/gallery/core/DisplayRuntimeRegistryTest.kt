package plutoproject.feature.gallery.core

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.display.ReplaceRuntimeImageDataResult
import plutoproject.feature.gallery.core.display.StopDisplayRuntimeResult
import plutoproject.feature.gallery.core.display.job.StaticDisplayJob
import plutoproject.feature.gallery.core.image.Image
import plutoproject.feature.gallery.core.image.ImageData
import plutoproject.feature.gallery.core.image.ImageType

@OptIn(ExperimentalCoroutinesApi::class)
class DisplayRuntimeRegistryTest {
    @Test
    fun `display instance store should create get find and delete instances`() = runTest {
        val repo = InMemoryDisplayInstanceRepository()
        val runtime = newDisplayRuntime(this, instances = repo)
        val store = runtime.displayInstanceStore
        val first = sampleDisplayInstance(id = dummyUuid(4002), imageId = dummyUuid(4001), chunkX = 10, chunkZ = 20)
        val second = sampleDisplayInstance(id = dummyUuid(4003), imageId = first.imageId, chunkX = 10, chunkZ = 20)

        assertTrue(store.create(first))
        assertTrue(store.create(second))
        assertEquals(false, store.create(first))

        assertSame(first, store.get(first.id))
        assertEquals(setOf(first.id, second.id), store.findByImageId(first.imageId).map { it.id }.toSet())
        assertEquals(setOf(first.id, second.id), store.findByChunk(10, 20).map { it.id }.toSet())

        val deleted = store.delete(first.id)
        assertSame(first, deleted)
        assertNull(store.get(first.id))
        assertNull(store.delete(first.id))

        runtime.runtimeRegistry.close()
        runtime.sendJobRegistry.close()
        runtime.scheduler.stop()
    }

    @Test
    fun `runtime registry should create reuse update and stop jobs`() = runTest {
        val runtime = newDisplayRuntime(this, clock = schedulerClock(this))
        val image = sampleImage(id = dummyUuid(4010))
        val data = sampleStaticImageData()
        val displayInstance = sampleDisplayInstance(imageId = image.id)

        val firstJob = runtime.runtimeRegistry.attach(image, data, displayInstance)
        val secondJob = runtime.runtimeRegistry.attach(image, data, sampleDisplayInstance(id = dummyUuid(4011), imageId = image.id))

        assertTrue(firstJob is StaticDisplayJob)
        assertSame(firstJob, secondJob)
        assertSame(firstJob, runtime.runtimeRegistry.getJob(image.id))

        val replacement = sampleStaticImageData()
        val replaceResult = runtime.runtimeRegistry.replaceImageData(image.id, replacement)
        assertEquals(ReplaceRuntimeImageDataResult.Updated(firstJob), replaceResult)

        val detached = runtime.runtimeRegistry.detach(image.id, displayInstance.id)
        assertNotNull(detached)
        assertSame(firstJob, runtime.runtimeRegistry.getJob(image.id))

        val stopResult = runtime.runtimeRegistry.stop(image.id)
        assertEquals(StopDisplayRuntimeResult.Stopped(firstJob), stopResult)
        assertNull(runtime.runtimeRegistry.getJob(image.id))
        assertEquals(ReplaceRuntimeImageDataResult.NotRunning, runtime.runtimeRegistry.replaceImageData(image.id, replacement))
        assertEquals(StopDisplayRuntimeResult.NotRunning, runtime.runtimeRegistry.stop(image.id))

        runtime.runtimeRegistry.close()
        runtime.sendJobRegistry.close()
        runtime.scheduler.stop()
        advanceUntilIdle()
    }

    @Test
    fun `send job registry should reuse and stop jobs`() = runTest {
        val runtime = newDisplayRuntime(this)
        val playerId = dummyUuid(4020)

        val first = runtime.sendJobRegistry.start(playerId)
        val second = runtime.sendJobRegistry.start(playerId)

        assertSame(first, second)
        assertSame(first, runtime.sendJobRegistry.get(playerId))
        assertSame(first, runtime.sendJobRegistry.stop(playerId))
        assertNull(runtime.sendJobRegistry.get(playerId))
        assertNull(runtime.sendJobRegistry.stop(playerId))

        runtime.runtimeRegistry.close()
        runtime.sendJobRegistry.close()
        runtime.scheduler.stop()
    }

    @Test
    fun `runtime registry should reject image and data type mismatch`() = runTest {
        val runtime = newDisplayRuntime(this)
        val image = sampleImage(id = dummyUuid(4030))
        val displayInstance = sampleDisplayInstance(imageId = image.id)
        val animatedData = ImageData.Animated(
            tilePool = sampleStaticImageData().tilePool,
            tileIndexes = shortArrayOf(0),
            frameCount = 1,
            duration = kotlin.time.Duration.ZERO,
        )

        try {
            assertThrows(IllegalArgumentException::class.java) {
                runtime.runtimeRegistry.attach(
                image = Image(
                    id = image.id,
                    type = ImageType.STATIC,
                    owner = image.owner,
                    ownerName = image.ownerName,
                    name = image.name,
                    widthBlocks = image.widthBlocks,
                    heightBlocks = image.heightBlocks,
                    tileMapIds = image.tileMapIds,
                ),
                    data = animatedData,
                    instance = displayInstance,
                )
            }
        } finally {
            runtime.runtimeRegistry.close()
            runtime.sendJobRegistry.close()
            runtime.scheduler.stop()
        }
    }
}
