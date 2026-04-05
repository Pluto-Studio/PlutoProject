package plutoproject.feature.gallery.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.MapUpdate
import plutoproject.feature.gallery.core.display.job.DisplayJob
import plutoproject.feature.gallery.core.display.job.DisplayResource
import plutoproject.feature.gallery.core.image.ImageType
import java.util.UUID

class DisplayRuntimeContractTest {
    @Test
    fun `map update should require full tile pixel payload`() {
        assertThrows(IllegalArgumentException::class.java) {
            MapUpdate(mapId = 100, mapColors = ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT - 1))
        }
    }

    @Test
    fun `map update equality should include full pixel payload`() {
        val first = MapUpdate(mapId = 100, mapColors = ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 1 })
        val second = MapUpdate(mapId = 100, mapColors = ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 1 })
        val different = MapUpdate(mapId = 100, mapColors = ByteArray(MapUpdate.MAP_UPDATE_PIXEL_COUNT) { 2 })

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertFalse(first == different)
    }

    @Test
    fun `display job should reject attach after stop and allow detach no op`() {
        val job = FakeDisplayJob()

        job.stop()

        assertTrue(job.isStopped)
        assertThrows(IllegalStateException::class.java) {
            job.attach(sampleDisplayInstance(imageId = job.imageId))
        }
        assertNull(job.detach(dummyUuid(8101)))
        assertTrue(job.isEmpty())
    }

    private class FakeDisplayJob : DisplayJob {
        override val imageId: UUID = dummyUuid(8100)
        override val type = ImageType.STATIC
        override var isStopped: Boolean = false
        override val attachedDisplayInstances = linkedMapOf<UUID, DisplayInstance>()

        override fun attach(displayInstance: DisplayInstance) {
            check(!isStopped) { "DisplayJob is stopped" }
            require(displayInstance.imageId == imageId)
            attachedDisplayInstances[displayInstance.id] = displayInstance
        }

        override fun detach(displayInstanceId: UUID): DisplayInstance? {
            if (isStopped) {
                return null
            }

            return attachedDisplayInstances.remove(displayInstanceId)
        }

        override fun replaceResource(resource: DisplayResource) = Unit

        override fun isEmpty(): Boolean = attachedDisplayInstances.isEmpty()

        override fun wake() = Unit

        override fun stop() {
            if (isStopped) {
                return
            }

            isStopped = true
            attachedDisplayInstances.clear()
        }
    }
}
