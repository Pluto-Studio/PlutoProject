package plutoproject.feature.gallery.core

class AllocateMapIdUseCase(
    private val mapIdRange: MapIdRange,
    private val systemInformationRepository: SystemInformationRepository,
) {
    @Suppress("UNUSED")
    sealed class Result {
        class IdRangeOverflow(range: MapIdRange) : Result()
        class Success(ids: IntArray) : Result()
    }

    suspend fun execute(count: Int): Result {
        require(count >= 0) { "Count must be >= 0: $count" }
        if (count == 0) {
            return Result.Success(IntArray(0))
        }

        val allocatedLastId = systemInformationRepository.allocateMapIds(count, mapIdRange)
            ?: return Result.IdRangeOverflow(mapIdRange)
        val startId = allocatedLastId - count + 1
        return Result.Success(IntArray(count) { index -> startId + index })
    }
}
