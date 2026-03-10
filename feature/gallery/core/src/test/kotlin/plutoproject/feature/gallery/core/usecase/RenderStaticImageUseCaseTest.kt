package plutoproject.feature.gallery.core.usecase

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.render.RenderResult
import plutoproject.feature.gallery.core.render.RenderStatus

class RenderStaticImageUseCaseTest {
    @Test
    fun `should return invalid-tile-count when map blocks is non-positive`() = runTest {
        var invoked = false
        val useCase = RenderStaticImageUseCase(
            renderer = {
                invoked = true
                RenderResult.failed(RenderStatus.PIPELINE_FAILED)
            }
        )

        val result = useCase.execute(staticRequest(mapXBlocks = 0, mapYBlocks = 1))

        assertEquals(RenderStatus.INVALID_TILE_COUNT, result.status)
        assertNull(result.imageData)
        assertFalse(invoked)
    }

    @Test
    fun `should return tile-count-overflow when tile count overflows int`() = runTest {
        val useCase = RenderStaticImageUseCase(
            renderer = {
                RenderResult.failed(RenderStatus.PIPELINE_FAILED)
            }
        )

        val result = useCase.execute(staticRequest(mapXBlocks = Int.MAX_VALUE, mapYBlocks = 2))

        assertEquals(RenderStatus.TILE_COUNT_OVERFLOW, result.status)
        assertNull(result.imageData)
    }

    @Test
    fun `should pass through renderer failure status`() = runTest {
        val useCase = RenderStaticImageUseCase(
            renderer = {
                RenderResult.failed(RenderStatus.PIPELINE_FAILED)
            }
        )

        val result = useCase.execute(staticRequest())

        assertEquals(RenderStatus.PIPELINE_FAILED, result.status)
        assertNull(result.imageData)
    }

    @Test
    fun `should return inconsistent-render-result when succeed result has null image data`() = runTest {
        val useCase = RenderStaticImageUseCase(
            renderer = {
                RenderResult(
                    status = RenderStatus.SUCCEED,
                    imageData = null,
                )
            }
        )

        val result = useCase.execute(staticRequest())

        assertEquals(RenderStatus.INCONSISTENT_RENDER_RESULT, result.status)
        assertNull(result.imageData)
    }

    @Test
    fun `should return tile-indexes-length-mismatch when rendered tile index count is invalid`() = runTest {
        val useCase = RenderStaticImageUseCase(
            renderer = {
                RenderResult.succeed(staticImageData(tileIndexesSize = 3, uniqueTileCount = 1))
            }
        )

        val result = useCase.execute(staticRequest(mapXBlocks = 1, mapYBlocks = 1))

        assertEquals(RenderStatus.TILE_INDEXES_LENGTH_MISMATCH, result.status)
        assertNull(result.imageData)
    }

    @Test
    fun `should return unique-tile-overflow when rendered unique tile count exceeds limit`() = runTest {
        val useCase = RenderStaticImageUseCase(
            renderer = {
                RenderResult.succeed(
                    staticImageData(
                        tileIndexesSize = 1,
                        uniqueTileCount = MAX_UNIQUE_TILE_COUNT + 1,
                    )
                )
            }
        )

        val result = useCase.execute(staticRequest(mapXBlocks = 1, mapYBlocks = 1))

        assertEquals(RenderStatus.UNIQUE_TILE_OVERFLOW, result.status)
        assertNull(result.imageData)
    }

    @Test
    fun `should return succeed when renderer output passes validation`() = runTest {
        val renderedData = staticImageData(tileIndexesSize = 4, uniqueTileCount = 2)
        val useCase = RenderStaticImageUseCase(
            renderer = {
                RenderResult.succeed(renderedData)
            }
        )

        val result = useCase.execute(staticRequest(mapXBlocks = 2, mapYBlocks = 2))

        assertEquals(RenderStatus.SUCCEED, result.status)
        assertTrue(result.imageData != null)
        assertSame(renderedData, result.imageData)
    }
}
