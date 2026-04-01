package plutoproject.feature.gallery.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.display.DisplayGeometry
import plutoproject.feature.gallery.core.display.DisplayInstance
import plutoproject.feature.gallery.core.display.ItemFrameFacing
import plutoproject.feature.gallery.core.display.PlayerView
import plutoproject.feature.gallery.core.display.TileRect
import plutoproject.feature.gallery.core.display.Vec3

class DisplayGeometryTest {
    @Test
    fun `should fall back to center ray when corner rays exceed visible distance`() {
        val geometry = sampleGeometry(widthBlocks = 1, heightBlocks = 1)

        val rect = geometry.computeVisibleTiles(
            playerViews = listOf(playerView(eye = Vec3(0.0, 0.0, 1.0))),
            visibleDistance = 1.1,
        ).values.singleOrNull()

        assertNotNull(rect)
        assertEquals(TileRect(minX = 0, maxX = 0, minY = 0, maxY = 0), rect)
    }

    @Test
    fun `should conservatively expand visible rect under default field of view`() {
        val geometry = sampleGeometry(widthBlocks = 3, heightBlocks = 3)

        val rect = geometry.computeVisibleTiles(
            playerViews = listOf(playerView(eye = Vec3(0.75, -1.25, 1.0))),
            visibleDistance = 2.0,
        ).values.singleOrNull()

        assertEquals(TileRect(minX = 0, maxX = 2, minY = 0, maxY = 2), rect)
    }

    @Test
    fun `should conservatively include both tiles when hit lands on shared boundary`() {
        val geometry = sampleGeometry(widthBlocks = 3, heightBlocks = 3)

        val rect = geometry.computeVisibleTiles(
            playerViews = listOf(playerView(eye = Vec3(0.5, -0.5, 1.0))),
            visibleDistance = 2.0,
        ).values.singleOrNull()

        assertEquals(TileRect(minX = 0, maxX = 1, minY = 0, maxY = 1), rect)
    }
}

private fun sampleGeometry(widthBlocks: Int, heightBlocks: Int): DisplayGeometry {
    return DisplayInstance(
        id = dummyUuid(7001),
        belongsTo = dummyUuid(7002),
        world = "world",
        chunkX = 0,
        chunkZ = 0,
        facing = ItemFrameFacing.SOUTH,
        widthBlocks = widthBlocks,
        heightBlocks = heightBlocks,
        originX = 0.0,
        originY = 0.0,
        originZ = 0.0,
        itemFrameIds = emptyList(),
    ).buildGeometry()
}

private fun playerView(eye: Vec3): PlayerView {
    return PlayerView(
        id = dummyUuid(7003),
        eye = eye,
        viewDirection = Vec3(0.0, 0.0, -1.0),
    )
}
