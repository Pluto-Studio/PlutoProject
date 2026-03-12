package plutoproject.feature.gallery.core.usecase

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import plutoproject.feature.gallery.core.render.RenderResult
import plutoproject.feature.gallery.core.render.RenderStatus

class RenderAnimatedImageUseCaseTest {
    @Test
    fun `should return invalid-source-frame-count when request frame list is empty`() = runTest {
        var invoked = false
        val useCase = RenderAnimatedImageUseCase(
            renderer = {
                invoked = true
                RenderResult.failed(RenderStatus.PIPELINE_FAILED)
            }
        )

        val result = useCase.execute(animatedRequest(sourceFrameCount = 0))

        assertTrue(result is RenderResult.Failure)
        assertEquals(RenderStatus.INVALID_SOURCE_FRAME_COUNT, result.status)
        assertFalse(invoked)
    }

    @Test
    fun `should return invalid-tile-count when map blocks is non-positive`() = runTest {
        val useCase = RenderAnimatedImageUseCase(
            renderer = {
                RenderResult.failed(RenderStatus.PIPELINE_FAILED)
            }
        )

        val result = useCase.execute(animatedRequest(sourceFrameCount = 1, mapXBlocks = 1, mapYBlocks = 0))

        assertTrue(result is RenderResult.Failure)
        assertEquals(RenderStatus.INVALID_TILE_COUNT, result.status)
    }

    @Test
    fun `should return tile-count-overflow when tile count overflows int`() = runTest {
        val useCase = RenderAnimatedImageUseCase(
            renderer = {
                RenderResult.failed(RenderStatus.PIPELINE_FAILED)
            }
        )

        val result = useCase.execute(animatedRequest(sourceFrameCount = 1, mapXBlocks = Int.MAX_VALUE, mapYBlocks = 2))

        assertTrue(result is RenderResult.Failure)
        assertEquals(RenderStatus.TILE_COUNT_OVERFLOW, result.status)
    }

    @Test
    fun `should pass through renderer failure status`() = runTest {
        val useCase = RenderAnimatedImageUseCase(
            renderer = {
                RenderResult.failed(RenderStatus.PIPELINE_FAILED)
            }
        )

        val result = useCase.execute(animatedRequest())

        assertTrue(result is RenderResult.Failure)
        assertEquals(RenderStatus.PIPELINE_FAILED, result.status)
    }

    @Test
    fun `should return inconsistent-render-result when succeed result has null image data`() = runTest {
        val useCase = RenderAnimatedImageUseCase(
            renderer = {
                RenderResult.Success(
                    imageData = null,
                )
            }
        )

        val result = useCase.execute(animatedRequest())

        assertTrue(result is RenderResult.Failure)
        assertEquals(RenderStatus.INCONSISTENT_RENDER_RESULT, result.status)
    }

    @Test
    fun `should return invalid-rendered-frame-count when rendered frame count is non-positive`() = runTest {
        val useCase = RenderAnimatedImageUseCase(
            renderer = {
                RenderResult.succeed(
                    animatedImageData(
                        frameCount = 0,
                        durationMillis = 100,
                        tileIndexesSize = 0,
                    )
                )
            }
        )

        val result = useCase.execute(animatedRequest())

        assertTrue(result is RenderResult.Failure)
        assertEquals(RenderStatus.INVALID_RENDERED_FRAME_COUNT, result.status)
    }

    @Test
    fun `should return invalid-rendered-duration-millis when rendered duration is non-positive`() = runTest {
        val useCase = RenderAnimatedImageUseCase(
            renderer = {
                RenderResult.succeed(
                    animatedImageData(
                        frameCount = 1,
                        durationMillis = 0,
                        tileIndexesSize = 1,
                    )
                )
            }
        )

        val result = useCase.execute(animatedRequest())

        assertTrue(result is RenderResult.Failure)
        assertEquals(RenderStatus.INVALID_RENDERED_DURATION_MILLIS, result.status)
    }

    @Test
    fun `should return tile-indexes-length-overflow when expected tile index count overflows int`() = runTest {
        val useCase = RenderAnimatedImageUseCase(
            renderer = {
                RenderResult.succeed(
                    animatedImageData(
                        frameCount = 50_000,
                        durationMillis = 100,
                        tileIndexesSize = 0,
                    )
                )
            }
        )

        val result = useCase.execute(animatedRequest(sourceFrameCount = 1, mapXBlocks = 50_000, mapYBlocks = 1))

        assertTrue(result is RenderResult.Failure)
        assertEquals(RenderStatus.TILE_INDEXES_LENGTH_OVERFLOW, result.status)
    }

    @Test
    fun `should return tile-indexes-length-mismatch when rendered tile index count is invalid`() = runTest {
        val useCase = RenderAnimatedImageUseCase(
            renderer = {
                RenderResult.succeed(
                    animatedImageData(
                        frameCount = 2,
                        durationMillis = 100,
                        tileIndexesSize = 1,
                    )
                )
            }
        )

        val result = useCase.execute(animatedRequest(sourceFrameCount = 1, mapXBlocks = 1, mapYBlocks = 1))

        assertTrue(result is RenderResult.Failure)
        assertEquals(RenderStatus.TILE_INDEXES_LENGTH_MISMATCH, result.status)
    }

    @Test
    fun `should return unique-tile-overflow when rendered unique tile count exceeds limit`() = runTest {
        val useCase = RenderAnimatedImageUseCase(
            renderer = {
                RenderResult.succeed(
                    animatedImageData(
                        frameCount = 1,
                        durationMillis = 100,
                        tileIndexesSize = 1,
                        uniqueTileCount = MAX_UNIQUE_TILE_COUNT + 1,
                    )
                )
            }
        )

        val result = useCase.execute(animatedRequest())

        assertTrue(result is RenderResult.Failure)
        assertEquals(RenderStatus.UNIQUE_TILE_OVERFLOW, result.status)
    }

    @Test
    fun `should return succeed when renderer output passes validation`() = runTest {
        val renderedData = animatedImageData(
            frameCount = 3,
            durationMillis = 120,
            tileIndexesSize = 12,
            uniqueTileCount = 3,
        )
        val useCase = RenderAnimatedImageUseCase(
            renderer = {
                RenderResult.succeed(renderedData)
            }
        )

        val result = useCase.execute(animatedRequest(sourceFrameCount = 2, mapXBlocks = 2, mapYBlocks = 2))

        assertTrue(result is RenderResult.Success)
        assertEquals(RenderStatus.SUCCEED, result.status)
        assertSame(renderedData, (result as RenderResult.Success).imageData)
    }
}
