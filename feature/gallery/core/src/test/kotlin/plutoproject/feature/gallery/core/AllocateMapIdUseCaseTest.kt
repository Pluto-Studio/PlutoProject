package plutoproject.feature.gallery.core

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
class AllocateMapIdUseCaseTest {
    @Test
    fun `allocate should return contiguous ids from start when no history exists`() = runTest {
        val repo = InMemorySystemInformationRepository()
        val useCase = AllocateMapIdUseCase(
            mapIdRange = MapIdRange(start = 100, end = 200),
            systemInformationRepository = repo,
        )

        val ids = (useCase.execute(5) as AllocateMapIdUseCase.Result.Success).ids

        assertArrayEquals(intArrayOf(100, 101, 102, 103, 104), ids)
        assertEquals(104, repo.lastAllocatedId)
    }

    @Test
    fun `allocate should continue from last allocated id`() = runTest {
        val repo = InMemorySystemInformationRepository(initialLastAllocatedId = 100)
        val useCase = AllocateMapIdUseCase(
            mapIdRange = MapIdRange(start = 1, end = 200),
            systemInformationRepository = repo,
        )

        val ids = (useCase.execute(5) as AllocateMapIdUseCase.Result.Success).ids

        assertArrayEquals(intArrayOf(101, 102, 103, 104, 105), ids)
        assertEquals(105, repo.lastAllocatedId)
    }

    @Test
    fun `allocate should throw IllegalStateException when range overflows`() = runTest {
        val repo = InMemorySystemInformationRepository(initialLastAllocatedId = 105)
        val useCase = AllocateMapIdUseCase(
            mapIdRange = MapIdRange(start = 100, end = 109),
            systemInformationRepository = repo,
        )

        val result = useCase.execute(5)
        assertTrue(result is AllocateMapIdUseCase.Result.IdRangeOverflow)
    }
}
