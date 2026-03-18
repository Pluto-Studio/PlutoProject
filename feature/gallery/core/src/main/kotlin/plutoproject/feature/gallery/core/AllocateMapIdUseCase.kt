package plutoproject.feature.gallery.core

class AllocateMapIdUseCase(
    private val allocationRange: AllocationRange,
    private val systemInformationRepository: SystemInformationRepository,
) {
    suspend fun execute(count: Int): IntArray {
        require(count >= 0) { "count must be >= 0: $count" }
        if (count == 0) return IntArray(0)

        val allocatedLastId = systemInformationRepository.allocateMapIds(count, allocationRange)
        check(allocatedLastId != null) {
            "map id allocation overflow: range=${allocationRange.start}..${allocationRange.end}, count=$count"
        }

        val startId = allocatedLastId - count + 1
        return IntArray(count) { index -> startId + index }
    }
}
